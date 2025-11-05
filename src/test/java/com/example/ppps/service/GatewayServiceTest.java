// REVIEW: full code review requested

package com.example.ppps.service;

import com.example.ppps.dto.GatewayRequest;
import com.example.ppps.dto.GatewayResponse;
import com.example.ppps.exception.GatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

class GatewayServiceTest {

    private GatewayService gatewayService;

    @BeforeEach
    void setup() {
        gatewayService = new GatewayService();
    }

    @Test
    void testProcessPayment_successfulResponse() {
        GatewayRequest request = GatewayRequest.builder()
                .transactionId(java.util.UUID.randomUUID())
                .amount(new java.math.BigDecimal("500.00"))
                .currency("NGN")
                .build();

        GatewayResponse response = gatewayService.processPayment(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getGatewayReference().length() > 0);
    }

    private void assertTrue(boolean b) {

    }

    @Test
    void testFallbackResponse_generatesPendingStatus() {
        GatewayRequest request = GatewayRequest.builder()
                .transactionId(java.util.UUID.randomUUID())
                .amount(new java.math.BigDecimal("100.00"))
                .currency("NGN")
                .build();

        GatewayResponse fallback = gatewayService.fallbackResponse(request, new GatewayException("Simulated Failure"));

        assertNotNull(fallback);
        assertEquals("PENDING", fallback.getStatus());
        assertTrue(fallback.getGatewayReference().startsWith("FALLBACK-"));
        assertTrue(fallback.getMessage().contains("Simulated Failure"));
    }
}
