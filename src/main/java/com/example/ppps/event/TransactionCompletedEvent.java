package com.example.ppps.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor

public class TransactionCompletedEvent {
    private UUID transactionId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private BigDecimal amount;
    private String status;
    private Instant completedAt;
}
