package com.example.ppps.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
public class WebhookWithdrawalRequest {

    @NotBlank(message = "External reference is required")
    private String externalReference;

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @NotBlank(message = "Status is required")
    private String status; // SUCCESS, FAILED, PENDING

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String bankName;

    private String accountNumber;

    private String accountName;

    private Instant processedAt;

    private String failureReason;

    private String bankReference;

    private Map<String, Object> metadata;
}