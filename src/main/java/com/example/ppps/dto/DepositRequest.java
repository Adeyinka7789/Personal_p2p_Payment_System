package com.example.ppps.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class DepositRequest {
    private UUID walletId;
    private BigDecimal amount;
}

