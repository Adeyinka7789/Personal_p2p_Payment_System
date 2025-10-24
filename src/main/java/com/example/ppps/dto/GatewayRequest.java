package com.example.ppps.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayRequest {
    private UUID transactionId;
    private BigDecimal amount;
    private String currency;
    private Map<String, Object> metadata;
}
