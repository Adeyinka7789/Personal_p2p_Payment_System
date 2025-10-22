package com.example.ppps.service;

import com.example.ppps.controller.BalanceResponse;
import com.example.ppps.entity.Wallet;
import com.example.ppps.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class BalanceServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getBalance_existingWallet_returnsCorrectBalance() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(BigDecimal.valueOf(1000.00));
        wallet.setCurrency("NGN");

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        BalanceResponse response = balanceService.getBalance(walletId);

        assertEquals(BigDecimal.valueOf(1000.00), response.getBalance());
        assertEquals("NGN", response.getCurrency());
    }
}