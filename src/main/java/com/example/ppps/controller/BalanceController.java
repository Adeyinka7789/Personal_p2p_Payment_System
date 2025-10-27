package com.example.ppps.controller;

import com.example.ppps.entity.User;
import com.example.ppps.entity.Wallet;
import com.example.ppps.repository.UserRepository;
import com.example.ppps.repository.WalletRepository;
import com.example.ppps.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class BalanceController {

    private final BalanceService balanceService;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    /**
     * ✅ Get the logged-in user's wallet balance
     */
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(Authentication authentication) {
        try {
            // Step 1: Get user_id from JWT
            String userId = authentication.getName();
            log.info("Fetching balance for user: {}", userId);

            // Step 2: Find the User in DB
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // Step 3: Get Wallet from User
            Wallet wallet = user.getWallet();
            if (wallet == null) {
                log.error("User {} has no wallet", userId);
                return ResponseEntity.badRequest().body(createErrorResponse("User wallet not found"));
            }

            // Step 4: Get balance directly from wallet (avoid service call that might cause issues)
            UUID walletId = wallet.getId();

            // Create response manually to avoid any serialization issues
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");

            Map<String, Object> balanceData = new HashMap<>();
            balanceData.put("amount", wallet.getBalance()); // Get directly from entity
            balanceData.put("currency", wallet.getCurrency());
            balanceData.put("walletId", walletId.toString());

            response.put("balance", balanceData);

            log.info("Balance fetched successfully for user {}: {} {}",
                    userId, wallet.getBalance(), wallet.getCurrency());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Balance fetch error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error fetching balance", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Error fetching balance: " + e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        return error;
    }
}