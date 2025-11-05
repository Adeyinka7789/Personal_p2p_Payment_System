// REVIEW: full code review requested

package com.example.ppps.controller;

import com.example.ppps.service.BalanceService;
import com.example.ppps.service.TransactionHistoryService;
import com.example.ppps.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TransferController {

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransactionHistoryService transactionHistoryService;

    @Autowired
    private BalanceService balanceService;

    @PostMapping("/transfers")
    public ResponseEntity<?> transfer(@RequestBody TransferRequest request) {
        try {
            transferService.executeP2PTransfer(
                    request.getReceiverPhoneNumber(),
                    request.getAmount(),
                    request.getSecurePin(),
                    request.getNarration()
            );
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Transfer completed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/balance/{walletId}")
    public BalanceResponse getBalance(@PathVariable UUID walletId) {
        return balanceService.getBalance(walletId);
    }

    @GetMapping("/transactions/{walletId}")
    public List<TransactionHistoryResponse> getTransactionHistory(
            @PathVariable UUID walletId,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        TransactionSearchRequest filters = new TransactionSearchRequest();
        filters.setStartDate(startDate);
        filters.setEndDate(endDate);
        filters.setStatus(status);
        filters.setMinAmount(minAmount);
        filters.setMaxAmount(maxAmount);
        filters.setPageNumber(pageNumber);
        filters.setPageSize(pageSize);
        return transactionHistoryService.getTransactionsForWallet(walletId, filters);
    }
}