package com.example.ppps.service;

import com.example.ppps.dto.WebhookDepositRequest;
import com.example.ppps.dto.WebhookWithdrawalRequest;
import com.example.ppps.entity.Transaction;
import com.example.ppps.entity.Wallet;
import com.example.ppps.enums.TransactionStatus;
import com.example.ppps.event.DepositCompletedEvent;
import com.example.ppps.event.WithdrawalCompletedEvent;
import com.example.ppps.exception.PppsException;
import com.example.ppps.repository.TransactionRepository;
import com.example.ppps.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebhookSecurityService webhookSecurityService;

    // process deposit webhook from external payment providers
    @Transactional
    public void processDepositWebhook(WebhookDepositRequest request, String signature) {
        log.info("🔄 Processing deposit webhook - Reference: {}, Status: {}",
                request.getExternalReference(), request.getStatus());

        webhookSecurityService.verifySignature(request, signature);

        //find or create transaction
        Transaction transaction = transactionRepository.findById(UUID.fromString(request.getTransactionId()))
                .orElseThrow(() -> new PppsException(HttpStatus.NOT_FOUND,
                        "Transaction not found: " + request.getTransactionId()));

        // process it based on status
        switch (request.getStatus().toUpperCase()) {
            case "SUCCESS":
                handleSuccessfulDeposit(transaction, request);
                break;
            case "FAILED":
                handleFailedDeposit(transaction, request);
                break;
            case "PENDING":
                handlePendingDeposit(transaction, request);
                break;
            default:
                throw new PppsException(HttpStatus.BAD_REQUEST,
                        "Invalid status: " + request.getStatus());
        }
        log.info("✅ Deposit webhook processed - Transaction: {}, Status: {}",
                transaction.getId(), request.getStatus());
    }

    // process withdrawal webhook from bank/processor
    @Transactional
    public void processWithdrawalWebhook(WebhookWithdrawalRequest request, String signature) {
        log.info("🔄 Processing withdrawal webhook - Reference: {}, Status: {}",
                request.getExternalReference(), request.getStatus());

        webhookSecurityService.verifySignature(request, signature);

        // find transaction
        Transaction transaction = transactionRepository.findById(UUID.fromString(request.getTransactionId()))
                .orElseThrow(() -> new PppsException(HttpStatus.NOT_FOUND,
                        "Transaction not found: " + request.getTransactionId()));

        // process based on status
        switch (request.getStatus().toUpperCase()) {
            case "SUCCESS":
                handleSuccessfulWithdrawal(transaction, request);
                break;
            case "FAILED":
                handleFailedWithdrawal(transaction, request);
                break;
            case "PENDING":
                handlePendingWithdrawal(transaction, request);
                break;
            default:
                throw new PppsException(HttpStatus.BAD_REQUEST,
                        "Invalid status: " + request.getStatus());
        }
        log.info("✅ Withdrawal webhook processed - Transaction: {}, Status: {}",
                transaction.getId(), request.getStatus());
    }

    private void handleSuccessfulDeposit(Transaction transaction, WebhookDepositRequest request) {
        // update transaction status
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);

        // credit the wallet
        Wallet wallet = walletRepository.findByIdWithLock(transaction.getReceiverWalletId());
        if (wallet != null) {
            BigDecimal newBalance = wallet.getBalance().add(request.getAmount());
            wallet.setBalance(newBalance);
            walletRepository.save(wallet);
            log.info("💰 Wallet credited - Wallet: {}, Amount: {}, New Balance: {}",
                    wallet.getId(), request.getAmount(), newBalance);
        }

        // publish deposit completed event
        kafkaTemplate.send("deposit.completed", transaction.getId().toString(),
                new DepositCompletedEvent(
                        transaction.getId(),
                        transaction.getReceiverWalletId(),
                        request.getAmount(),
                        request.getExternalReference(),
                        "DEPOSIT_SUCCESS",
                        Instant.now()
                ));
    }

    private void handleFailedDeposit(Transaction transaction, WebhookDepositRequest request) {
        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
        log.warn("❌ Deposit failed - Transaction: {}, Reason: {}",
                transaction.getId(), request.getFailureReason());
    }

    private void handlePendingDeposit(Transaction transaction, WebhookDepositRequest request) {
        transaction.setStatus(TransactionStatus.PENDING);
        transactionRepository.save(transaction);
        log.info("⏳ Deposit pending - Transaction: {}", transaction.getId());
    }

    private void handleSuccessfulWithdrawal(Transaction transaction, WebhookWithdrawalRequest request) {
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);
        // publish withdrawal completed event
        kafkaTemplate.send("withdrawal.completed", transaction.getId().toString(),
                new WithdrawalCompletedEvent(
                        transaction.getId(),
                        transaction.getSenderWalletId(),
                        request.getAmount(),
                        request.getBankName(),
                        request.getAccountNumber(),
                        "WITHDRAWAL_SUCCESS",
                        Instant.now()
                ));
        log.info("💸 Withdrawal completed - Transaction: {}, Bank Ref: {}",
                transaction.getId(), request.getBankReference());
    }

    private void handleFailedWithdrawal(Transaction transaction, WebhookWithdrawalRequest request) {
        // Refund the wallet if withdrawal failed
        Wallet wallet = walletRepository.findByIdWithLock(transaction.getSenderWalletId());
        if (wallet != null) {
            BigDecimal refundAmount = transaction.getAmount();
            BigDecimal newBalance = wallet.getBalance().add(refundAmount);
            wallet.setBalance(newBalance);
            walletRepository.save(wallet);
            log.info("💳 Withdrawal refunded - Wallet: {}, Amount: {}, New Balance: {}",
                    wallet.getId(), refundAmount, newBalance);
        }

        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
        log.warn("❌ Withdrawal failed - Transaction: {}, Reason: {}",
                transaction.getId(), request.getFailureReason());
    }

    private void handlePendingWithdrawal(Transaction transaction, WebhookWithdrawalRequest request) {
        transaction.setStatus(TransactionStatus.PENDING);
        transactionRepository.save(transaction);
        log.info("⏳ Withdrawal pending - Transaction: {}", transaction.getId());
    }
}