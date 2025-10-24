package com.example.ppps.service;

import com.example.ppps.dto.GatewayRequest;
import com.example.ppps.dto.GatewayResponse;
import com.example.ppps.entity.*;
import com.example.ppps.event.TransactionCompletedEvent;
import com.example.ppps.exception.*;
import com.example.ppps.repository.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class TransferService {

    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final Timer transferTimer;
    private final PasswordEncoder passwordEncoder;
    private final FeeService feeService;
    private final GatewayService gatewayService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UUID platformWalletId;

    // ✅ FIX #1: Use configurable topic from application.properties
    @Value("${app.kafka.topic.transactions.completed:transactions.completed}")
    private String transactionCompletedTopic;

    public TransferService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            UserRepository userRepository,
            EntityManager entityManager,
            MeterRegistry meterRegistry,
            PasswordEncoder passwordEncoder,
            FeeService feeService,
            GatewayService gatewayService,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.platform-wallet-id}") String platformWalletIdStr) {

        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
        this.feeService = feeService;
        this.gatewayService = gatewayService;
        this.kafkaTemplate = kafkaTemplate;

        this.transferTimer = Timer.builder("ppps.transfer.duration")
                .description("Time taken to process a P2P transfer")
                .register(meterRegistry);

        this.platformWalletId = (platformWalletIdStr == null || platformWalletIdStr.isBlank())
                ? null : UUID.fromString(platformWalletIdStr);
    }

    @Transactional
    public void executeP2PTransfer(String receiverPhoneNumber, BigDecimal amount, String securePin, String narration) {
        transferTimer.record(() -> {
            String correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);

            try {
                logger.info("🚀 Starting P2P transfer - Receiver: {}, Amount: {}", receiverPhoneNumber, amount);

                // ========================================
                // 1️⃣ Authentication & User Validation
                // ========================================
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null) {
                    throw new PppsException(HttpStatus.UNAUTHORIZED, "No authentication context");
                }

                String userId = authentication.getPrincipal().toString();
                logger.debug("Authenticated user ID: {}", userId);

                User senderUser = userRepository.findById(userId)
                        .orElseThrow(() -> new PppsException(HttpStatus.NOT_FOUND, "Sender user not found"));

                User receiverUser = userRepository.findByPhoneNumber(receiverPhoneNumber)
                        .orElseThrow(() -> new PppsException(HttpStatus.NOT_FOUND,
                                "Receiver with phone number " + receiverPhoneNumber + " not found"));

                logger.info("✅ Sender: {} | Receiver: {}", senderUser.getPhoneNumber(), receiverUser.getPhoneNumber());

                // ========================================
                // 2️⃣ Wallet Validation
                // ========================================
                Wallet senderWallet = senderUser.getWallet();
                Wallet receiverWallet = receiverUser.getWallet();

                if (senderWallet == null || receiverWallet == null) {
                    throw new PppsException(HttpStatus.BAD_REQUEST, "Both users must have wallets");
                }

                UUID senderWalletId = senderWallet.getId();
                UUID receiverWalletId = receiverWallet.getId();

                if (senderWalletId.equals(receiverWalletId)) {
                    throw new PppsException(HttpStatus.BAD_REQUEST, "Cannot transfer to yourself");
                }

                logger.debug("Sender Wallet ID: {} | Receiver Wallet ID: {}", senderWalletId, receiverWalletId);

                // ========================================
                // 3️⃣ PIN Verification
                // ========================================
                if (securePin == null || securePin.trim().isEmpty()) {
                    throw new PppsException(HttpStatus.BAD_REQUEST, "PIN is required");
                }

                if (!verifyPin(securePin, senderUser.getHashedPin())) {
                    logger.warn("⚠️ Invalid PIN attempt for user: {}", userId);
                    throw new PppsException(HttpStatus.UNAUTHORIZED, "Invalid PIN");
                }

                logger.debug("✅ PIN verified successfully");

                // ========================================
                // 4️⃣ Amount Validation & Fee Calculation
                // ========================================
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new PppsException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
                }

                BigDecimal fee = feeService.calculateFee(amount);
                if (fee == null) fee = BigDecimal.ZERO;
                BigDecimal totalDebit = amount.add(fee);

                logger.info("💰 Amount: {} | Fee: {} | Total Debit: {}", amount, fee, totalDebit);

                // ========================================
                // 5️⃣ Lock Wallets (Pessimistic Locking)
                // ========================================
                logger.debug("🔒 Acquiring locks on wallets...");

                Wallet senderWalletLocked = walletRepository.findByIdWithLock(senderWalletId);
                Wallet receiverWalletLocked = walletRepository.findByIdWithLock(receiverWalletId);

                if (senderWalletLocked == null || receiverWalletLocked == null) {
                    throw new WalletNotFoundException("Wallet lock failed");
                }

                Wallet platformWallet = null;
                if (fee.compareTo(BigDecimal.ZERO) > 0) {
                    if (platformWalletId == null) {
                        throw new PppsException(HttpStatus.INTERNAL_SERVER_ERROR, "Platform wallet not configured");
                    }
                    platformWallet = walletRepository.findByIdWithLock(platformWalletId);
                    if (platformWallet == null) {
                        throw new PppsException(HttpStatus.INTERNAL_SERVER_ERROR, "Platform wallet not found");
                    }
                }

                logger.debug("✅ Wallets locked successfully");

                // ========================================
                // 6️⃣ Balance Check
                // ========================================
                if (senderWalletLocked.getBalance().compareTo(totalDebit) < 0) {
                    logger.warn("⚠️ Insufficient funds - Required: {}, Available: {}",
                            totalDebit, senderWalletLocked.getBalance());
                    throw new InsufficientFundsException(
                            String.format("Insufficient funds. Required: %.2f, Available: %.2f",
                                    totalDebit, senderWalletLocked.getBalance())
                    );
                }

                // ========================================
                // 7️⃣ Update Balances (ACID Transaction)
                // ========================================
                BigDecimal senderOldBalance = senderWalletLocked.getBalance();
                BigDecimal receiverOldBalance = receiverWalletLocked.getBalance();

                senderWalletLocked.setBalance(senderWalletLocked.getBalance().subtract(totalDebit));
                receiverWalletLocked.setBalance(receiverWalletLocked.getBalance().add(amount));

                if (platformWallet != null) {
                    platformWallet.setBalance(platformWallet.getBalance().add(fee));
                }

                walletRepository.saveAndFlush(senderWalletLocked);
                walletRepository.saveAndFlush(receiverWalletLocked);
                if (platformWallet != null) {
                    walletRepository.saveAndFlush(platformWallet);
                }

                logger.info("💸 Balances updated - Sender: {} → {} | Receiver: {} → {}",
                        senderOldBalance, senderWalletLocked.getBalance(),
                        receiverOldBalance, receiverWalletLocked.getBalance());

                // ========================================
                // 8️⃣ Create Transaction Record
                // ========================================
                Transaction transaction = new Transaction();
                transaction.setSenderWalletId(senderWalletId);
                transaction.setReceiverWalletId(receiverWalletId);
                transaction.setAmount(amount);
                transaction.setStatus(TransactionStatus.PENDING);
                transaction.setInitiatedAt(Instant.now());
                transaction = transactionRepository.saveAndFlush(transaction);

                logger.info("📝 Transaction record created - ID: {}", transaction.getId());

                // ========================================
                // 9️⃣ Create Ledger Entries (Double-Entry Bookkeeping)
                // ========================================
                createLedgerEntry(transaction.getId(), senderWalletId, totalDebit, EntryType.DEBIT);
                createLedgerEntry(transaction.getId(), receiverWalletId, amount, EntryType.CREDIT);

                if (fee.compareTo(BigDecimal.ZERO) > 0) {
                    createLedgerEntry(transaction.getId(), platformWalletId, fee, EntryType.FEE_REVENUE);
                    logger.debug("💳 Fee ledger entry created - Amount: {}", fee);
                }

                entityManager.flush();
                logger.debug("✅ Ledger entries persisted");

                // ========================================
                // 🔟 Gateway Payment Processing
                // ========================================
                logger.info("🌐 Calling payment gateway...");

                GatewayRequest gatewayRequest = GatewayRequest.builder()
                        .transactionId(transaction.getId())
                        .amount(amount)
                        .currency("NGN")
                        .metadata(Map.of(
                                "senderWallet", senderWalletId.toString(),
                                "receiverWallet", receiverWalletId.toString(),
                                "narration", narration != null ? narration : "",
                                "correlationId", correlationId))
                        .build();

                GatewayResponse gatewayResponse = gatewayService.processPayment(gatewayRequest);
                logger.info("✅ Gateway response received - Status: {}", gatewayResponse.getStatus());

                // ========================================
                // 1️⃣1️⃣ Update Transaction Status
                // ========================================
                if ("SUCCESS".equalsIgnoreCase(gatewayResponse.getStatus())) {
                    transaction.setStatus(TransactionStatus.SUCCESS);
                    logger.info("✅ Transaction marked as SUCCESS");

                    // ✅ FIX #2 & #3: Publish to Kafka AFTER transaction commit with proper error handling
                    Transaction finalTransaction = transaction;
                    UUID finalSenderWalletId = senderWalletId;
                    UUID finalReceiverWalletId = receiverWalletId;
                    BigDecimal finalAmount = amount;
                    String finalCorrelationId = correlationId;

                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publishTransactionCompletedEvent(
                                    finalTransaction,
                                    finalSenderWalletId,
                                    finalReceiverWalletId,
                                    finalAmount,
                                    finalCorrelationId
                            );
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                logger.error("❌ Transaction rolled back - ID: {} | CorrelationId: {}",
                                        finalTransaction.getId(), finalCorrelationId);
                            }
                        }
                    });

                } else if ("PENDING".equalsIgnoreCase(gatewayResponse.getStatus())) {
                    transaction.setStatus(TransactionStatus.PENDING);
                    logger.warn("⏳ Transaction status: PENDING - Manual verification may be required");
                } else {
                    transaction.setStatus(TransactionStatus.FAILED);
                    logger.error("❌ Transaction FAILED - Gateway status: {}", gatewayResponse.getStatus());
                }

                transactionRepository.saveAndFlush(transaction);
                logger.info("✅ P2P Transfer completed successfully - Transaction ID: {}", transaction.getId());

            } catch (InsufficientFundsException | WalletNotFoundException | PppsException e) {
                logger.error("❌ Transfer failed - {}: {}", e.getClass().getSimpleName(), e.getMessage());
                throw e;
            } catch (Exception e) {
                logger.error("❌ Unexpected error during transfer - CorrelationId: {}", correlationId, e);
                throw new PppsException(HttpStatus.INTERNAL_SERVER_ERROR, "Transaction failed: " + e.getMessage());
            } finally {
                MDC.clear();
            }
        });
    }

    /**
     * ✅ FIX #3: Robust Kafka publishing with success/failure logging
     * Publishes TransactionCompletedEvent to Kafka after transaction commit.
     * Exceptions are caught and logged to prevent breaking the main flow.
     */
    private void publishTransactionCompletedEvent(
            Transaction transaction,
            UUID senderWalletId,
            UUID receiverWalletId,
            BigDecimal amount,
            String correlationId) {

        try {
            logger.info("📤 Publishing TransactionCompletedEvent to Kafka - Tx ID: {} | Topic: {}",
                    transaction.getId(), transactionCompletedTopic);

            TransactionCompletedEvent event = new TransactionCompletedEvent(
                    transaction.getId(),
                    senderWalletId,
                    receiverWalletId,
                    amount,
                    transaction.getStatus().name(),
                    Instant.now()
            );

            // ✅ FIX #1: Use configurable topic instead of hard-coded constant
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    transactionCompletedTopic,
                    transaction.getId().toString(),
                    event
            );

            // ✅ Add success/failure callbacks
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("✅ Kafka message sent successfully - Tx ID: {} | Partition: {} | Offset: {} | Topic: {}",
                            transaction.getId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            transactionCompletedTopic);
                } else {
                    logger.error("❌ Failed to publish Kafka message - Tx ID: {} | Topic: {} | CorrelationId: {} | Error: {}",
                            transaction.getId(),
                            transactionCompletedTopic,
                            correlationId,
                            ex.getMessage(),
                            ex);
                    // IMPORTANT: Do NOT throw exception here - transaction already committed
                    // Consider implementing a dead-letter queue or retry mechanism
                }
            });

        } catch (Exception e) {
            // ✅ Catch any synchronous exceptions to prevent breaking the flow
            logger.error("❌ Exception during Kafka publish attempt - Tx ID: {} | Topic: {} | CorrelationId: {}",
                    transaction.getId(),
                    transactionCompletedTopic,
                    correlationId,
                    e);
            // Transaction is already committed - do NOT re-throw
            // Consider alerting ops team or implementing compensating actions
        }
    }

    /**
     * Creates a ledger entry for double-entry bookkeeping.
     */
    private void createLedgerEntry(UUID transactionId, UUID walletId, BigDecimal amount, EntryType type) {
        LedgerEntry entry = new LedgerEntry();
        entry.setTransactionId(transactionId);
        entry.setWalletId(walletId);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setCreatedAt(Instant.now());
        ledgerEntryRepository.saveAndFlush(entry);

        logger.debug("📘 Ledger entry created - Type: {} | Wallet: {} | Amount: {}",
                type, walletId, amount);
    }

    /**
     * Verifies user's PIN using bcrypt password encoder.
     */
    private boolean verifyPin(String providedPin, String hashedPin) {
        if (hashedPin == null) {
            throw new PppsException(HttpStatus.BAD_REQUEST, "User PIN not set");
        }
        return passwordEncoder.matches(providedPin, hashedPin);
    }
}