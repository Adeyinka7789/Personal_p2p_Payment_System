package com.example.ppps.controller;

import com.example.ppps.entity.User;
import com.example.ppps.entity.Wallet;
import com.example.ppps.repository.UserRepository;
import com.example.ppps.repository.WalletRepository;
import com.example.ppps.service.AuthenticationService;
import com.example.ppps.service.BalanceService;
import com.example.ppps.service.TransactionHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private TransactionHistoryService transactionHistoryService;

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        try {
            logger.info("Registration attempt for phone: {}", request.getPhoneNumber());

            // Validate email format if provided
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                if (!isValidEmail(request.getEmail())) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Invalid email format"));
                }
            }

            AuthenticationResponse response = authenticationService.register(request);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("status", "success");
            successResponse.put("message", "User registered successfully");
            successResponse.put("token", response.getToken());
            successResponse.put("userId", response.getUserId());

            logger.info("User registered successfully: {}", request.getPhoneNumber());
            return ResponseEntity.ok(successResponse);

        } catch (RuntimeException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Registration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Registration failed due to server error"));
        }
    }

    /**
     * User login
     */
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            logger.info("Login attempt for phone: {}", request.getPhoneNumber());

            AuthenticationResponse response = authenticationService.authenticate(
                    request.getPhoneNumber(),
                    request.getPin()
            );

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("status", "success");
            successResponse.put("message", "Login successful");
            successResponse.put("token", response.getToken());
            successResponse.put("userId", response.getUserId());

            logger.info("User logged in successfully: {}", request.getPhoneNumber());
            return ResponseEntity.ok(successResponse);

        } catch (SecurityException e) {
            logger.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Login failed due to server error"));
        }
    }

    @GetMapping("/user/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            logger.warn("No authentication context for /user/me");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("No authentication context"));
        }

        String userId = auth.getName();
        logger.info("Fetching user info for: {}", userId);

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                logger.warn("User not found for userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("User not found"));
            }

            User user = userOpt.get();

            // Create clean response with email
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getUserId());
            userData.put("phoneNumber", user.getPhoneNumber());
            userData.put("email", user.getEmail()); // Include email in response

            // Add wallet ID if available
            if (user.getWallet() != null) {
                userData.put("walletId", user.getWallet().getId().toString());
            }

            response.put("user", userData);

            logger.info("Retrieved user: {}", user.getPhoneNumber());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching user info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get transactions for the authenticated user
     */
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            logger.warn("No authentication context for /transactions");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("No authentication context"));
        }

        String userId = auth.getName();
        logger.info("Fetching transactions for user: {}", userId);

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                logger.warn("User not found for userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("User not found"));
            }

            User user = userOpt.get();
            Wallet wallet = user.getWallet();

            if (wallet == null) {
                logger.warn("Wallet not found for userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Wallet not found"));
            }

            // Create search filters
            TransactionSearchRequest filters = new TransactionSearchRequest();
            filters.setStartDate(startDate);
            filters.setEndDate(endDate);
            filters.setStatus(status);
            filters.setMinAmount(minAmount);
            filters.setMaxAmount(maxAmount);
            filters.setPageNumber(pageNumber);
            filters.setPageSize(pageSize);

            List<TransactionHistoryResponse> transactions;
            try {
                transactions = transactionHistoryService.getTransactionsForWallet(wallet.getId(), filters);
            } catch (Exception queryEx) {
                // If query fails, return empty list
                logger.warn("Transaction query failed, returning empty list: {}", queryEx.getMessage());
                transactions = List.of();
            }

            // Format transactions for frontend
            List<Map<String, Object>> formattedTransactions = transactions.stream()
                    .map(t -> {
                        // Determine transaction type based on sender/receiver
                        boolean isSender = t.getSenderWalletId() != null &&
                                t.getSenderWalletId().equals(wallet.getId());
                        String type = isSender ? "P2P_SENT" : "P2P_RECEIVED";

                        Map<String, Object> map = new HashMap<>();
                        map.put("id", t.getTransactionId().toString());
                        map.put("type", type);
                        map.put("amount", t.getAmount());
                        map.put("status", t.getStatus());
                        map.put("createdAt", t.getInitiatedAt() != null ? t.getInitiatedAt().toString() : "");
                        map.put("narration", isSender ?
                                "Sent to wallet " + (t.getReceiverWalletId() != null ? t.getReceiverWalletId().toString() : "Unknown") :
                                "Received from wallet " + (t.getSenderWalletId() != null ? t.getSenderWalletId().toString() : "Unknown"));

                        return map;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("transactions", formattedTransactions);

            logger.info("Retrieved {} transactions for user: {}",
                    formattedTransactions.size(), userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving transactions", e);
            // Return empty transactions instead of error
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("transactions", List.of());
            return ResponseEntity.ok(response);
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email != null && email.matches(emailRegex);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        return error;
    }
}