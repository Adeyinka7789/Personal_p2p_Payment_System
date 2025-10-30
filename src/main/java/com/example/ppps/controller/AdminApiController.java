package com.example.ppps.controller;

import com.example.ppps.entity.User;
import com.example.ppps.entity.Wallet;
import com.example.ppps.entity.Transaction;
import com.example.ppps.enums.TransactionStatus;
import com.example.ppps.repository.UserRepository;
import com.example.ppps.repository.WalletRepository;
import com.example.ppps.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userDtos = users.stream().map(user -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", user.getUserId());
            dto.put("phoneNumber", user.getPhoneNumber());
            dto.put("role", user.getRole());
            dto.put("walletId", user.getWallet() != null ? user.getWallet().getId() : null);
            dto.put("active", true);
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/wallets")
    public ResponseEntity<List<Map<String, Object>>> getAllWallets() {
        List<Wallet> wallets = walletRepository.findAll();
        List<Map<String, Object>> walletDtos = wallets.stream().map(wallet -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", wallet.getId());
            dto.put("userId", wallet.getUserId().toString());
            dto.put("balance", wallet.getBalance());
            dto.put("currency", wallet.getCurrency());

            // Get user phone for display
            String userPhone = userRepository.findById(wallet.getUserId().toString())
                    .map(User::getPhoneNumber)
                    .orElse("Unknown");
            dto.put("userPhone", userPhone);

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(walletDtos);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Map<String, Object>>> getAllTransactions() {
        List<Transaction> transactions = transactionRepository.findAll();
        List<Map<String, Object>> transactionDtos = transactions.stream().map(transaction -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", transaction.getId());

            // Get sender user info
            String senderUserId = getUserIdFromWallet(transaction.getSenderWalletId());
            String senderPhone = getPhoneFromWallet(transaction.getSenderWalletId());
            dto.put("userId", senderUserId);
            dto.put("userPhone", senderPhone);

            dto.put("amount", transaction.getAmount());
            dto.put("status", transaction.getStatus());
            dto.put("type", "TRANSFER");
            dto.put("createdAt", transaction.getInitiatedAt());

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(transactionDtos);
    }

    @GetMapping("/withdrawals")
    public ResponseEntity<List<Map<String, Object>>> getWithdrawals(@RequestParam(required = false) String status) {
        List<Transaction> transactions;

        if (status != null && !status.isEmpty()) {
            try {
                TransactionStatus transactionStatus = TransactionStatus.valueOf(status.toUpperCase());
                transactions = transactionRepository.findByStatus(transactionStatus);
            } catch (IllegalArgumentException e) {
                // If invalid status, return empty list
                transactions = Collections.emptyList();
            }
        } else {
            // Get all PENDING transactions for withdrawals view
            transactions = transactionRepository.findByStatus(TransactionStatus.PENDING);
        }

        List<Map<String, Object>> withdrawalDtos = transactions.stream().map(transaction -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", transaction.getId().toString());

            // Get sender user info
            String senderUserId = getUserIdFromWallet(transaction.getSenderWalletId());
            String senderPhone = getPhoneFromWallet(transaction.getSenderWalletId());
            dto.put("userId", senderUserId);
            dto.put("userPhone", senderPhone);

            dto.put("amount", transaction.getAmount());
            dto.put("status", transaction.getStatus());
            dto.put("createdAt", transaction.getInitiatedAt());

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(withdrawalDtos);
    }

    @PutMapping("/withdrawals/{id}")
    public ResponseEntity<?> updateWithdrawalStatus(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            Transaction transaction = transactionRepository.findById(UUID.fromString(id))
                    .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + id));

            // Update transaction status
            TransactionStatus newStatus = TransactionStatus.valueOf(status.toUpperCase());
            transaction.setStatus(newStatus);
            transactionRepository.save(transaction);

            return ResponseEntity.ok(Map.of(
                    "message", "Transaction status updated successfully",
                    "transactionId", id,
                    "newStatus", newStatus
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid status: " + request.get("status")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error updating transaction: " + e.getMessage()));
        }
    }

    // Helper methods to get user info from wallet
    private String getUserIdFromWallet(UUID walletId) {
        if (walletId == null) return "Unknown";

        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet != null) {
            return wallet.getUserId().toString();
        }
        return "Unknown";
    }

    private String getPhoneFromWallet(UUID walletId) {
        if (walletId == null) return "Unknown";

        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet != null) {
            User user = userRepository.findById(wallet.getUserId().toString()).orElse(null);
            if (user != null) {
                return user.getPhoneNumber();
            }
        }
        return "Unknown";
    }
}