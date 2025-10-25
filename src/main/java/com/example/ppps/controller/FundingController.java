package com.example.ppps.controller;

import com.example.ppps.dto.DepositRequest;
import com.example.ppps.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/funding")
@RequiredArgsConstructor
@Slf4j
public class FundingController {

    private final BalanceService balanceService;

    @PostMapping
    public ResponseEntity<String> deposit(
            @RequestBody DepositRequest request,
            Authentication authentication
    ) {
        String correlationId = MDC.get("correlationId");
        try {
            if (request.getWalletId() == null) {
                return ResponseEntity.badRequest().body("Wallet ID is required.");
            }
            if (request.getAmount() == null || request.getAmount().signum() <= 0) {
                return ResponseEntity.badRequest().body("Deposit amount must be greater than zero.");
            }

            UUID walletId = request.getWalletId();
            String user = authentication != null ? authentication.getName() : "anonymous";

            log.info("[{}] 💰 Deposit initiated by {} for wallet: {} amount: {}", correlationId, user, walletId, request.getAmount());

            balanceService.depositFunds(walletId, request.getAmount());

            log.info("[{}] ✅ Deposit successful for wallet {}", correlationId, walletId);
            return ResponseEntity.ok("✅ Deposit successful for wallet: " + walletId);

        } catch (IllegalArgumentException e) {
            log.error("[{}] ⚠️ Deposit failed due to invalid argument: {}", correlationId, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("[{}] ❌ Deposit failed unexpectedly: {}", correlationId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Unexpected error: " + e.getMessage());
        }
    }
}