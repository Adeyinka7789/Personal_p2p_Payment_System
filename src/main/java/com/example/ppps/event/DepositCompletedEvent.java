package com.example.ppps.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositCompletedEvent {
    private UUID transactionId;
    private UUID walletId;
    private BigDecimal amount;
    private String externalReference;
    private String status;
    private Instant completedAt;
}