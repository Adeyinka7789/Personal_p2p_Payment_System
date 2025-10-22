package com.example.ppps.controller;

import com.example.ppps.dto.DepositRequest;
import com.example.ppps.service.BalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/funding")
@RequiredArgsConstructor
public class FundingController {

    private final BalanceService balanceService;

    // A temporary utility endpoint for testing/deposits
    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestBody DepositRequest request) {
        // You would need to add a method like 'depositFunds' to your BalanceService
        balanceService.depositFunds(request.getWalletId(), request.getAmount());
        return ResponseEntity.ok("Deposit successful for wallet: " + request.getWalletId());
    }
}

