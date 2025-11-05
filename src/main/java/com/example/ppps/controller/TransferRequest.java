// REVIEW: full code review requested

package com.example.ppps.controller;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    private String senderId;
    private String receiverPhoneNumber;
    private BigDecimal amount;
    private String securePin;
    private String narration;
}