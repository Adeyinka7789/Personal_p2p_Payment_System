package com.example.ppps.service;

import com.example.ppps.controller.BalanceResponse;
import com.example.ppps.entity.Wallet;
import com.example.ppps.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final WalletRepository walletRepository;
    private final AuditLogService auditLogService; // Inject the AuditLog service

    /**
     * Retrieves the current balance for a given wallet ID.
     * @param walletId The ID of the wallet to check.
     * @return A BalanceResponse object containing the current balance and currency.
     * @throws IllegalArgumentException if the wallet is not found.
     */
    @Cacheable(value = "balances", key = "#walletId")
    public BalanceResponse getBalance(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        BalanceResponse response = new BalanceResponse();
        response.setBalance(wallet.getBalance());
        response.setCurrency(wallet.getCurrency());
        return response;
    }

    /**
     * Adds funds to a specified wallet. This is used for simulation/testing
     * of external funding sources like banks.
     * @param walletId The ID of the wallet to deposit funds into.
     * @param amount The amount to deposit.
     */
    public void depositFunds(UUID walletId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for deposit: " + walletId));

        BigDecimal oldBalance = wallet.getBalance();
        BigDecimal newBalance = oldBalance.add(amount);
        wallet.setBalance(newBalance);

        walletRepository.save(wallet);

        // ✅ Record audit log for this wallet update
        auditLogService.recordAction(
                "SYSTEM", // or current user/admin ID if available
                "DEPOSIT_FUNDS",
                "Wallet",
                walletId.toString(),
                String.format("Deposited ₦%s. Balance changed from ₦%s to ₦%s", amount, oldBalance, newBalance)
        );
    }
}