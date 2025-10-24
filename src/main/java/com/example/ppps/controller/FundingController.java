package com.example.ppps.controller;

import com.example.ppps.dto.DepositRequest;
import com.example.ppps.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/funding")
@RequiredArgsConstructor
public class FundingController {

    private final BalanceService balanceService;

    @PostMapping
    public ResponseEntity<String> deposit(
            @RequestBody DepositRequest request,
            Authentication authentication
    ) {
        try {
            // ✅ Validate request
            if (request.getWalletId() == null) {
                return ResponseEntity.badRequest().body("Wallet ID is required.");
            }

            if (request.getAmount() == null || request.getAmount().signum() <= 0) {
                return ResponseEntity.badRequest().body("Deposit amount must be greater than zero.");
            }

            // ✅ Use walletId directly from the request
            UUID walletId = request.getWalletId();

            // Optionally log the authenticated user for traceability
            String user = authentication != null ? authentication.getName() : "anonymous";
            System.out.println("💰 Deposit initiated by: " + user + " for wallet: " + walletId);

            // ✅ Perform deposit
            balanceService.depositFunds(walletId, request.getAmount());

            return ResponseEntity.ok("✅ Deposit successful for wallet: " + walletId);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Unexpected error: " + e.getMessage());
        }
    }
}