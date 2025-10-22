package com.example.ppps.controller;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    // Note: senderId will be ignored - we get it from JWT authentication
    private String senderId;  // Optional - for backwards compatibility
    private String receiverPhoneNumber;  // This is what you actually use!
    private BigDecimal amount;
    private String securePin;
    private String narration;  // Optional transaction description
}