package com.example.ppps.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayResponse {
    private String status;            // SUCCESS, FAILED, PENDING
    private String gatewayReference;  // e.g., mock external reference
    private String message;
}

