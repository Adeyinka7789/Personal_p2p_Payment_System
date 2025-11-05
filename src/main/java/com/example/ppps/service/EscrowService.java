// REVIEW: full code review requested

package com.example.ppps.service;

import com.example.ppps.entity.LedgerEntry;
import com.example.ppps.entity.Transaction;
import com.example.ppps.entity.Wallet;
import com.example.ppps.enums.EntryType;
import com.example.ppps.enums.TransactionStatus;
import com.example.ppps.repository.LedgerEntryRepository;
import com.example.ppps.repository.TransactionRepository;
import com.example.ppps.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class EscrowService {

    private static final Logger logger = LoggerFactory.getLogger(EscrowService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    // check if transfer should be in escrow, pegged it at > 50K
    public boolean requiresEscrow(BigDecimal amount) {
        return amount.compareTo(new BigDecimal("50000.00")) >= 0;
    }

    //kindly create an escrow hold : ensure sender has sufficient balance
    @Transactional
    public void createEscrowHold(UUID transactionId, BigDecimal amount) {
        try {
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
            // lock sender wallet to validate balance
            Wallet senderWallet = walletRepository.findByIdWithLock(transaction.getSenderWalletId());
            if (senderWallet == null) {
                throw new RuntimeException("Sender wallet not found");
            }

            // check if sender has sufficient balance for the escrow hold
            if (senderWallet.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance for escrow hold");
            }

            logger.info("✅ Escrow hold validated - Transaction: {}, Amount: {}, Sender Balance: {}",
                    transactionId, amount, senderWallet.getBalance());

        } catch (Exception e) {
            logger.error("❌ Error creating escrow hold for transaction: {}", transactionId, e);
            throw new RuntimeException("Failed to create escrow hold: " + e.getMessage());
        }
    }

    // auto-complete pending transactions after 30 minutes
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCompletePendingTransactions() {
        Instant thirtyMinutesAgo = Instant.now().minus(30, ChronoUnit.MINUTES);

        List<Transaction> pendingTransactions = transactionRepository
                .findByStatusAndInitiatedAtBefore(TransactionStatus.PENDING, thirtyMinutesAgo);

        for (Transaction transaction : pendingTransactions) {
            completeEscrowTransaction(transaction);
        }

        if (!pendingTransactions.isEmpty()) {
            logger.info("Auto-completed {} pending transactions", pendingTransactions.size());
        }
    }

    // move fund and update status
    @Transactional
    public void completeEscrowTransaction(Transaction transaction) {
        try {
            // lock both wallet
            Wallet senderWallet = walletRepository.findByIdWithLock(transaction.getSenderWalletId());
            Wallet receiverWallet = walletRepository.findByIdWithLock(transaction.getReceiverWalletId());

            if (senderWallet == null || receiverWallet == null) {
                logger.error("Wallet not found for transaction: {}", transaction.getId());
                return;
            }

            // verify again if sender has sufficient balance; needless but should be true always
            if (senderWallet.getBalance().compareTo(transaction.getAmount()) < 0) {
                logger.error("Insufficient balance to complete escrow - Transaction: {}, Sender: {}, Balance: {}, Amount: {}",
                        transaction.getId(), senderWallet.getId(), senderWallet.getBalance(), transaction.getAmount());
                return;
            }

            // transfer the principal amount from sender to receiver
            senderWallet.setBalance(senderWallet.getBalance().subtract(transaction.getAmount()));
            receiverWallet.setBalance(receiverWallet.getBalance().add(transaction.getAmount()));

            walletRepository.save(senderWallet);
            walletRepository.save(receiverWallet);

            // update transaction status
            transaction.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);

            // kindly create ledger entries for the principal transfer
            createLedgerEntry(transaction.getId(), senderWallet.getId(), transaction.getAmount(), EntryType.DEBIT);
            createLedgerEntry(transaction.getId(), receiverWallet.getId(), transaction.getAmount(), EntryType.CREDIT);

            logger.info("Escrow completed - Transaction: {}, Amount: {}, Sender: {}, Receiver: {}",
                    transaction.getId(), transaction.getAmount(), senderWallet.getId(), receiverWallet.getId());

        } catch (Exception e) {
            logger.error("Error completing escrow transaction: {}", transaction.getId(), e);
        }
    }

    // cancel a pending escrow transaction -- no fund movement needed since amount was never deducted
    @Transactional
    public void cancelEscrowTransaction(UUID transactionId, UUID senderWalletId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // validate ownership and status
        if (!transaction.getSenderWalletId().equals(senderWalletId)) {
            throw new RuntimeException("Not authorized to cancel this transaction");
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Only pending transactions can be cancelled");
        }

        // is it within the 30-minute window?
        Instant thirtyMinutesAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
        if (transaction.getInitiatedAt().isBefore(thirtyMinutesAgo)) {
            throw new RuntimeException("Cancellation period has expired");
        }

        // kindly update status to cancelled - no need to refund since amount was never deducted
        transaction.setStatus(TransactionStatus.CANCELLED);
        transactionRepository.save(transaction);

        logger.info("Escrow cancelled - Transaction: {}, Amount: {}", transactionId, transaction.getAmount());
    }

    // kindly create ledger entry for escrow completion
    private void createLedgerEntry(UUID transactionId, UUID walletId, BigDecimal amount, EntryType type) {
        LedgerEntry entry = new LedgerEntry();
        entry.setTransactionId(transactionId);
        entry.setWalletId(walletId);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setCreatedAt(Instant.now());
        ledgerEntryRepository.save(entry);

        logger.debug("Escrow ledger entry created - Type: {} | Wallet: {} | Amount: {}", type, walletId, amount);
    }
}