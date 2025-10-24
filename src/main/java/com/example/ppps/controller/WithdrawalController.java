package com.example.ppps.controller;

import com.example.ppps.dto.WithdrawRequest;
import com.example.ppps.dto.WithdrawalResponse;
import com.example.ppps.entity.TransactionStatus;
import com.example.ppps.service.WithdrawalService;
import jakarta.validation.Valid; // Required for DTO validation
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added for logging
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/withdrawals")
@RequiredArgsConstructor
@Slf4j // Enable logging
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    /**
     * Endpoint for users to initiate a withdrawal from their wallet to a linked bank account.
     * The transaction is initiated as PENDING and an asynchronous process (Kafka consumer)
     * will handle the final status update and notification.
     *
     * @param request The withdrawal details including amount, bank info, and PIN.
     * @param authentication Spring Security Authentication object containing the user's ID.
     * @return ResponseEntity with WithdrawalResponse and 202 ACCEPTED status.
     */
    @PostMapping
    public ResponseEntity<WithdrawalResponse> withdraw(@Valid @RequestBody WithdrawRequest request,
                                                       Authentication authentication) {

        String authUserId = authentication.getName();
        log.info("Withdrawal request received for user ID: {} to account {}", authUserId, request.getAccountNumber());

        // This call will debit the account and call the external gateway,
        // throwing an exception if PIN is invalid, balance is insufficient, etc.
        withdrawalService.withdraw(authUserId, request);

        // If the service succeeds, the transaction is created in the database,
        // and the Kafka event is registered for commit. The API should respond quickly.
        WithdrawalResponse response = new WithdrawalResponse(
                // In a real system, you would get the Transaction ID from the service layer
                "TXN-" + Instant.now().toEpochMilli(),
                request.getAmount(),
                request.getAccountNumber(),
                request.getBankName(),
                TransactionStatus.PENDING,
                Instant.now(),
                "Withdrawal request accepted. Funds transfer is now processing asynchronously."
        );

        // Use 202 ACCEPTED for requests that initiate an asynchronous process (like an external bank transfer)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}