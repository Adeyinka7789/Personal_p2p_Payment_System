package com.example.ppps.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor  // <-- THIS WAS MISSING!
public class TransactionHistoryResponse {
    private UUID transactionId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private BigDecimal amount;
    private String status;
    private Instant initiatedAt;
}