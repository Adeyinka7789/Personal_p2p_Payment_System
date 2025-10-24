package com.example.ppps.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionCompletedEvent {
    private UUID transactionId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private BigDecimal amount;
    private String status;
    private Instant completedAt;

    public TransactionCompletedEvent() {}

    public TransactionCompletedEvent(UUID transactionId, UUID senderWalletId, UUID receiverWalletId,
                                     BigDecimal amount, String status, Instant completedAt) {
        this.transactionId = transactionId;
        this.senderWalletId = senderWalletId;
        this.receiverWalletId = receiverWalletId;
        this.amount = amount;
        this.status = status;
        this.completedAt = completedAt;
    }

    // Getters and setters
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public UUID getSenderWalletId() { return senderWalletId; }
    public void setSenderWalletId(UUID senderWalletId) { this.senderWalletId = senderWalletId; }
    public UUID getReceiverWalletId() { return receiverWalletId; }
    public void setReceiverWalletId(UUID receiverWalletId) { this.receiverWalletId = receiverWalletId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
