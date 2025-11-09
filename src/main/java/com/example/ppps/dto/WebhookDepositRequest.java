package com.example.ppps.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
public class WebhookDepositRequest {

    @NotBlank(message = "External reference is required")
    private String externalReference;

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Status is required")
    private String status;

    private String walletId;

    private String userPhoneNumber;

    private Instant processedAt;

    private String failureReason;

    private Map<String, Object> metadata;
}