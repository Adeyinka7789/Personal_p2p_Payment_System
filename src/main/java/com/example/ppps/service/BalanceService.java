package com.example.ppps.service;

import com.example.ppps.controller.BalanceResponse;
import com.example.ppps.entity.Wallet;
import com.example.ppps.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal; // Import for handling currency amounts
import java.util.UUID;

@Service
public class BalanceService {

    @Autowired
    private WalletRepository walletRepository;

    /**
     * Retrieves the current balance for a given wallet ID.
     * * @param walletId The ID of the wallet to check.
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
     * * @param walletId The ID of the wallet to deposit funds into.
     * @param amount The amount to deposit.
     */
    public void depositFunds(UUID walletId, BigDecimal amount) {
        // 1. Validate the deposit amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        // 2. Find the wallet by ID
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for deposit: " + walletId));

        // 3. Calculate and set the new balance
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);

        // 4. Save the updated wallet to persist the new balance
        walletRepository.save(wallet);
    }
}