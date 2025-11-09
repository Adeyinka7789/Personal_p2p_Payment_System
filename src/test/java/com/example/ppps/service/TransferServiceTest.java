package com.example.ppps.service;

import com.example.ppps.entity.Wallet;
import com.example.ppps.exception.InsufficientFundsException;
import com.example.ppps.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class TransferServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void executeP2PTransfer_insufficientFunds_throwsException() {
        UUID senderWalletId = UUID.randomUUID();
        UUID receiverWalletId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(1500.00);

        Wallet senderWallet = new Wallet();
        senderWallet.setId(senderWalletId);
        senderWallet.setBalance(BigDecimal.valueOf(1000.00));

        when(walletRepository.findById(senderWalletId)).thenReturn(Optional.of(senderWallet));

        assertThrows(InsufficientFundsException.class, () ->
                transferService.executeP2PTransfer(senderWalletId, receiverWalletId, amount, "1234"));
    }
}