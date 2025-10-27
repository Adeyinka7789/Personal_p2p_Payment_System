package com.example.ppps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class WithdrawRequest {

    // Optional: if null, we infer wallet from authenticated user
    private UUID walletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    // Accept both "securePin" and "pin" from JSON
    @JsonProperty(value = "securePin", access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "PIN is required")
    private String securePin;

    // Allow "pin" as an alias for "securePin"
    @JsonProperty("pin")
    public void setPin(String pin) {
        this.securePin = pin;
    }

    private String narration;
}