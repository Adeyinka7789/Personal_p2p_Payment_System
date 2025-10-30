package com.example.ppps.controller;

import com.example.ppps.dto.WithdrawRequest;
import com.example.ppps.dto.WithdrawalResponse;
import com.example.ppps.enums.TransactionStatus;
import com.example.ppps.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    //Endpoint for users to initiate a withdrawal from their wallet to a linked bank account.
    @PostMapping
    public ResponseEntity<WithdrawalResponse> withdraw(@Valid @RequestBody WithdrawRequest request,
                                                       Authentication authentication) {
        String authUserId = authentication.getName();
        log.info("Withdrawal request received for user ID: {} to account {}", authUserId, request.getAccountNumber());
        // This call will debit the account and call the external gateway
        withdrawalService.withdraw(authUserId, request);
        // If the service succeeds, the transaction is created in the database
        WithdrawalResponse response = new WithdrawalResponse(
                "TXN-" + Instant.now().toEpochMilli(),
                request.getAmount(),
                request.getAccountNumber(),
                request.getBankName(),
                TransactionStatus.PENDING,
                Instant.now(),
                "Withdrawal request accepted. Funds transfer is now processing asynchronously."
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}