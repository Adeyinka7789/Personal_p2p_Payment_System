// REVIEW: full code review requested

package com.example.ppps.service;

import com.example.ppps.config.KafkaTopics;
import com.example.ppps.dto.GatewayRequest;
import com.example.ppps.dto.GatewayResponse;
import com.example.ppps.dto.WithdrawRequest;
import com.example.ppps.entity.*;
import com.example.ppps.enums.EntryType;
import com.example.ppps.enums.TransactionStatus;
import com.example.ppps.event.WithdrawalCompletedEvent;
import com.example.ppps.exception.*;
import com.example.ppps.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class WithdrawalService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final UserRepository userRepository;
    private final FeeService feeService;
    private final GatewayService gatewayService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PasswordEncoder passwordEncoder;
    private final UUID platformWalletId;

    @Value("${app.kafka.topic.transactions.completed:transactions.completed}")
    private String transactionTopic;

    public WithdrawalService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            UserRepository userRepository,
            FeeService feeService,
            GatewayService gatewayService,
            KafkaTemplate<String, Object> kafkaTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${app.platform-wallet-id}") String platformWalletIdStr) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.userRepository = userRepository;
        this.feeService = feeService;
        this.gatewayService = gatewayService;
        this.kafkaTemplate = kafkaTemplate;
        this.passwordEncoder = passwordEncoder;
        this.platformWalletId = (platformWalletIdStr == null || platformWalletIdStr.isBlank())
                ? null : UUID.fromString(platformWalletIdStr);
    }

    // let's perform a withdrawal from the authenticated user's wallet to external bank account

    @Transactional
    public void withdraw(String authenticatedUserId, WithdrawRequest request) {
        try {
            if (authenticatedUserId == null) {
                throw new PppsException(HttpStatus.UNAUTHORIZED, "No authentication context");
            }
            var user = userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> new PppsException(HttpStatus.NOT_FOUND, "User not found"));

            Wallet userWallet = user.getWallet();
            if (userWallet == null) {
                throw new PppsException(HttpStatus.BAD_REQUEST, "User has no wallet");
            }

            UUID walletIdToUse = request.getWalletId() == null ? userWallet.getId() : request.getWalletId();

            if (!walletIdToUse.equals(userWallet.getId())) {
                throw new PppsException(HttpStatus.FORBIDDEN, "Cannot withdraw from wallet you don't own");
            }

            if (request.getSecurePin() == null || request.getSecurePin().trim().isEmpty()) {
                throw new PppsException(HttpStatus.BAD_REQUEST, "PIN is required for withdrawal");
            }
            if (!verifyPin(request.getSecurePin(), user.getHashedPin())) {
                throw new PppsException(HttpStatus.UNAUTHORIZED, "Invalid PIN");
            }

            BigDecimal amount = request.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new PppsException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
            }

            BigDecimal fee = feeService.calculateFee(amount);
            if (fee == null) fee = BigDecimal.ZERO;
            BigDecimal totalDebit = amount.add(fee);

            //lock wallet (pessimistic)
            Wallet walletLocked = walletRepository.findByIdWithLock(walletIdToUse);
            if (walletLocked == null) throw new WalletNotFoundException("Wallet not found (locked)");

            // platform wallet for fee capture is required: to record fee revenue
            Wallet platformWallet = null;
            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                if (platformWalletId == null) {
                    throw new PppsException(HttpStatus.INTERNAL_SERVER_ERROR, "Platform wallet not configured to collect fees");
                }
                platformWallet = walletRepository.findByIdWithLock(platformWalletId);
                if (platformWallet == null) {
                    throw new PppsException(HttpStatus.INTERNAL_SERVER_ERROR, "Platform wallet not found to collect fees");
                }
            }

            // sufficient balance?
            if (walletLocked.getBalance().compareTo(totalDebit) < 0) {
                throw new InsufficientFundsException(String.format("Insufficient balance. Available: %s, Required: %s",
                        walletLocked.getBalance(), totalDebit));
            }

            // Deduct balance now (we'll commit only if the DB transaction succeeds)
            walletLocked.setBalance(walletLocked.getBalance().subtract(totalDebit));
            if (platformWallet != null) {
                platformWallet.setBalance(platformWallet.getBalance().add(fee));
            }
            walletRepository.saveAndFlush(walletLocked);
            if (platformWallet != null) walletRepository.saveAndFlush(platformWallet);

            //create transaction record
            Transaction tx = new Transaction();
            tx.setSenderWalletId(walletLocked.getId());
            // Note❗: for withdrawals we set receiverWalletId to platformWalletId--represents outflow
            tx.setReceiverWalletId(platformWalletId != null ? platformWalletId : walletLocked.getId());
            tx.setAmount(amount);
            tx.setStatus(TransactionStatus.PENDING);
            tx.setInitiatedAt(Instant.now());
            tx = transactionRepository.saveAndFlush(tx);

            //ledger entries -- DEBIT user for totalDebit, FEE_REVENUE if fee > 0
            LedgerEntry debit = new LedgerEntry();
            debit.setTransactionId(tx.getId());
            debit.setWalletId(walletLocked.getId());
            debit.setEntryType(EntryType.DEBIT);
            debit.setAmount(totalDebit);
            debit.setCreatedAt(Instant.now());
            ledgerEntryRepository.saveAndFlush(debit);

            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                LedgerEntry feeEntry = new LedgerEntry();
                feeEntry.setTransactionId(tx.getId());
                feeEntry.setWalletId(platformWalletId);
                feeEntry.setEntryType(EntryType.FEE_REVENUE);
                feeEntry.setAmount(fee);
                feeEntry.setCreatedAt(Instant.now());
                ledgerEntryRepository.saveAndFlush(feeEntry);
            }

            //Call external gateway to send funds to bank
            String correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
            // i used HashMap<String, Object> to allow null values
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("bankName", request.getBankName());
            metadata.put("accountNumber", request.getAccountNumber());
            metadata.put("narration", request.getNarration()); // Can be null

            GatewayRequest gatewayRequest = GatewayRequest.builder()
                    .transactionId(tx.getId())
                    .amount(amount)
                    .currency(walletLocked.getCurrency())
                    .metadata(metadata)
                    .build();

            GatewayResponse gatewayResponse = gatewayService.processWithdrawal(gatewayRequest);

            // Update tx status based on gateway response
            if ("SUCCESS".equalsIgnoreCase(gatewayResponse.getStatus())) {
                tx.setStatus(TransactionStatus.SUCCESS);
                Transaction finalTx = tx;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        //publish Withdrawal Completed Event to Kafka
                        kafkaTemplate.send(
                                KafkaTopics.WITHDRAWAL_COMPLETED,
                                finalTx.getId().toString(),
                                new WithdrawalCompletedEvent(
                                        finalTx.getId(),
                                        finalTx.getSenderWalletId(),
                                        finalTx.getAmount(),
                                        request.getBankName(),
                                        request.getAccountNumber(),
                                        "WITHDRAWAL_SUCCESS",
                                        Instant.now()
                                )
                        );
                        log.info("📤 Kafka WithdrawalCompletedEvent published for withdrawal tx {}", finalTx.getId());
                    }
                });
            } else if ("PENDING".equalsIgnoreCase(gatewayResponse.getStatus())) {
                tx.setStatus(TransactionStatus.PENDING);
            } else {
                tx.setStatus(TransactionStatus.FAILED);

                // revert balances if gateway failed
                walletLocked.setBalance(walletLocked.getBalance().add(totalDebit));
                walletRepository.saveAndFlush(walletLocked);
                if (platformWallet != null) {
                    platformWallet.setBalance(platformWallet.getBalance().subtract(fee));
                    walletRepository.saveAndFlush(platformWallet);
                }
            }

            transactionRepository.saveAndFlush(tx);
            MDC.clear();

        } catch (PppsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Withdrawal failed", e);
            throw new PppsException(HttpStatus.INTERNAL_SERVER_ERROR, "Withdrawal failed: " + e.getMessage());
        }
    }

    private boolean verifyPin(String providedPin, String hashedPin) {
        if (hashedPin == null) throw new PppsException(HttpStatus.BAD_REQUEST, "User PIN not set");
        return passwordEncoder.matches(providedPin, hashedPin);
    }
}