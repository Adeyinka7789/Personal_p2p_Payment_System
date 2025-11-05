// REVIEW: full code review requested

package com.example.ppps.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

class FeeServiceTest {

    private FeeService feeService;

    @BeforeEach
    void setup() {
        feeService = new FeeService();

        // Simulate @Value injections
        feeService.fixedFee = new BigDecimal("10.00");
        feeService.percentage = new BigDecimal("0.015");  // 1.5%
        feeService.scale = 2;
    }

    @Test
    void testCalculateFee_withValidAmount() {
        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal expected = new BigDecimal("25.00"); // 10 + (1.5% of 1000)
        BigDecimal actual = feeService.calculateFee(amount);

        assertEquals(expected, actual, "Fee should be correctly calculated");
    }

    @Test
    void testCalculateFee_withZeroAmount() {
        BigDecimal fee = feeService.calculateFee(BigDecimal.ZERO);
        assertEquals(new BigDecimal("10.00"), fee, "Fixed fee should apply even for zero amount");
    }

    @Test
    void testCalculateFee_withNullAmount() {
        BigDecimal fee = feeService.calculateFee(null);
        assertEquals(new BigDecimal("0.00"), fee, "Null amount should return zero fee");
    }

    @Test
    void testGetFeeConfiguration() {
        String config = feeService.getFeeConfiguration();
        assertTrue(config.contains("Fixed Fee"), "Configuration string should include labels");
    }
}
