package com.example.ppps.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;


@Data
public class WithdrawRequest {
    // Optional: if null, we infer wallet from authenticated user
    private UUID walletId;
    private BigDecimal amount;
    private String bankName;
    private String accountNumber;
    private String securePin;
    private String narration;
}
