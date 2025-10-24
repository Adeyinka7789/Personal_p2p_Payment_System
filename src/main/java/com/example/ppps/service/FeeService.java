package com.example.ppps.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class FeeService {

    @Value("${app.fee.fixed:0.00}")
    BigDecimal fixedFee;

    @Value("${app.fee.percentage:0.0}")
    BigDecimal percentage;

    @Value("${app.fee.scale:2}")
    int scale;

    /**
     * Calculate total transaction fee based on fixed and percentage rates.
     */
    public BigDecimal calculateFee(BigDecimal amount) {
        if (amount == null) {
            log.warn("🟡 FeeService: Null amount provided. Returning zero fee.");
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }

        BigDecimal safePercentage = percentage == null ? BigDecimal.ZERO : percentage;
        BigDecimal safeFixed = fixedFee == null ? BigDecimal.ZERO : fixedFee;

        BigDecimal percentPortion = amount.multiply(safePercentage);
        BigDecimal totalFee = safeFixed.add(percentPortion).setScale(scale, RoundingMode.HALF_UP);

        log.info("💰 Calculated fee: {} (Fixed: {}, %: {}) for amount {}", totalFee, safeFixed, safePercentage, amount);
        return totalFee;
    }

    /**
     * Debug helper for testing / config verification
     */
    public String getFeeConfiguration() {
        return String.format("Fixed Fee: %s | Percentage: %s | Scale: %d",
                fixedFee, percentage, scale);
    }
}