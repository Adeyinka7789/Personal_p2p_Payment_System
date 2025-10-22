package com.example.ppps.service;

import com.example.ppps.entity.LedgerEntry;
import com.example.ppps.entity.Transaction;
import com.example.ppps.entity.User;
import com.example.ppps.entity.Wallet;
import com.example.ppps.entity.EntryType;
import com.example.ppps.entity.TransactionStatus;
import com.example.ppps.exception.InsufficientFundsException;
import com.example.ppps.exception.WalletNotFoundException;
import com.example.ppps.exception.PppsException;
import com.example.ppps.repository.LedgerEntryRepository;
import com.example.ppps.repository.TransactionRepository;
import com.example.ppps.repository.UserRepository;
import com.example.ppps.repository.WalletRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TransferService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final Timer transferTimer;
    private final PasswordEncoder passwordEncoder;

    public TransferService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            UserRepository userRepository,
            EntityManager entityManager,
            MeterRegistry meterRegistry,
            PasswordEncoder passwordEncoder) {

        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;

        this.transferTimer = Timer.builder("ppps.transfer.duration")
                .description("Time taken to process a P2P transfer")
                .register(meterRegistry);
    }

    @Transactional
    public void executeP2PTransfer(String receiverPhoneNumber, BigDecimal amount, String securePin, String narration) {
        transferTimer.record(() -> {
            try {
                // 1. Get authenticated user
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null) {
                    throw new PppsException(HttpStatus.UNAUTHORIZED, "No authentication context");
                }

                String userId = authentication.getPrincipal().toString();
                System.out.println("\n--- P2P TRANSFER DEBUG ---");
                System.out.println("Authenticated user ID: " + userId);

                // 2. Find sender user by ID
                User senderUser = userRepository.findById(userId)
                        .orElseThrow(() -> new PppsException(HttpStatus.NOT_FOUND, "Sender user not found"));
                System.out.println("✅ Sender user found: " + senderUser.getPhoneNumber());

                // 3. Get sender's wallet from user relationship
                Wallet senderWallet = senderUser.getWallet();
                if (senderWallet == null) {
                    throw new PppsException(HttpStatus.BAD_REQUEST, "You don't have a wallet. Please create one first.");
                }

                UUID senderWalletId = senderWallet.getId();
                System.out.println("✅ Sender wallet ID: " + senderWalletId);
                System.out.println("   Sender balance: " + senderWallet.getBalance());

                // 4. Find receiver by phone number
                User receiverUser = (User) userRepository.findByPhoneNumber(receiverPhoneNumber)
                        .orElseThrow(() -> new PppsException(HttpStatus.NOT_FOUND,
                                "Receiver with phone number " + receiverPhoneNumber + " not found"));
                System.out.println("✅ Receiver user found: " + receiverUser.getPhoneNumber());

                // 5. Get receiver's wallet
                Wallet receiverWallet = receiverUser.getWallet();
                if (receiverWallet == null) {
                    throw new PppsException(HttpStatus.BAD_REQUEST,
                            "Receiver doesn't have a wallet");
                }

                UUID receiverWalletId = receiverWallet.getId();
                System.out.println("✅ Receiver wallet ID: " + receiverWalletId);
                System.out.println("   Receiver balance: " + receiverWallet.getBalance());
                System.out.println("Amount to transfer: " + amount);
                System.out.println("Narration: " + narration);
                System.out.println("--------------------------");

                // 6. Prevent self-transfer
                if (senderWalletId.equals(receiverWalletId)) {
                    throw new PppsException(HttpStatus.BAD_REQUEST, "Cannot transfer to yourself");
                }

                // 7. Verify PIN
                if (securePin == null || securePin.trim().isEmpty()) {
                    throw new PppsException(HttpStatus.BAD_REQUEST, "PIN is required");
                }

                if (!verifyPin(securePin, senderUser.getHashedPin())) {
                    throw new PppsException(HttpStatus.UNAUTHORIZED, "Invalid PIN");
                }
                System.out.println("✅ PIN verified");

                // 8. Validate amount
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new PppsException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
                }

                // 9. Check sufficient balance
                if (senderWallet.getBalance().compareTo(amount) < 0) {
                    throw new InsufficientFundsException(
                            String.format("Insufficient balance. Available: %.2f, Required: %.2f",
                                    senderWallet.getBalance(), amount)
                    );
                }

                // 10. Perform transfer (debit sender, credit receiver)
                senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
                receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
                walletRepository.save(senderWallet);
                walletRepository.save(receiverWallet);
                System.out.println("💰 Balances updated successfully.");
                System.out.println("   New sender balance: " + senderWallet.getBalance());
                System.out.println("   New receiver balance: " + receiverWallet.getBalance());

                // 11. Create transaction record
                Transaction transaction = new Transaction();
                transaction.setSenderWalletId(senderWalletId);
                transaction.setReceiverWalletId(receiverWalletId);
                transaction.setAmount(amount);
                transaction.setStatus(TransactionStatus.PENDING);
                transaction.setInitiatedAt(Instant.now());
                transaction = transactionRepository.save(transaction);
                System.out.println("📄 Transaction record created: " + transaction.getId());

                // 12. Create ledger entries (double-entry bookkeeping)
                LedgerEntry debitEntry = new LedgerEntry();
                debitEntry.setTransactionId(transaction.getId());
                debitEntry.setWalletId(senderWalletId);
                debitEntry.setEntryType(EntryType.DEBIT);
                debitEntry.setAmount(amount);
                debitEntry.setCreatedAt(Instant.now());
                ledgerEntryRepository.save(debitEntry);

                LedgerEntry creditEntry = new LedgerEntry();
                creditEntry.setTransactionId(transaction.getId());
                creditEntry.setWalletId(receiverWalletId);
                creditEntry.setEntryType(EntryType.CREDIT);
                creditEntry.setAmount(amount);
                creditEntry.setCreatedAt(Instant.now());
                ledgerEntryRepository.save(creditEntry);
                System.out.println("📘 Ledger entries saved (debit & credit).");

                // 13. Mark transaction as successful
                transaction.setStatus(TransactionStatus.SUCCESS);
                transactionRepository.save(transaction);
                System.out.println("✅ Transaction marked as SUCCESS.");
                System.out.println("✅ Transfer completed successfully!");

            } catch (InsufficientFundsException e) {
                throw new PppsException(HttpStatus.BAD_REQUEST, e.getMessage());
            } catch (WalletNotFoundException e) {
                throw new PppsException(HttpStatus.NOT_FOUND, e.getMessage());
            } catch (PppsException e) {
                throw e;  // Re-throw custom exceptions as-is
            } catch (Exception e) {
                e.printStackTrace();
                throw new PppsException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Transaction failed: " + e.getMessage());
            }
        });
    }

    private boolean verifyPin(String providedPin, String hashedPin) {
        if (hashedPin == null) {
            throw new PppsException(HttpStatus.BAD_REQUEST, "User PIN not set");
        }
        return passwordEncoder.matches(providedPin, hashedPin);
    }
}