package com.example.ppps.service;

import com.example.ppps.dto.GatewayResponse;
import com.example.ppps.entity.*;
import com.example.ppps.exception.PppsException;
import com.example.ppps.repository.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransferServiceTest1 {

    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FeeService feeService;
    @Mock private GatewayService gatewayService;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    private MeterRegistry meterRegistry;
    @InjectMocks private TransferService transferService;

    private UUID senderWalletId;
    private UUID receiverWalletId;
    private User senderUser;
    private User receiverUser;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        meterRegistry = new SimpleMeterRegistry();

        // recreate transferService with meterRegistry and no platformWalletId
        transferService = new TransferService(
                walletRepository, transactionRepository, ledgerEntryRepository, userRepository,
                null, meterRegistry, passwordEncoder, feeService, gatewayService, null
        );

        // setup sender and receiver
        senderWalletId = UUID.randomUUID();
        receiverWalletId = UUID.randomUUID();

        senderWallet = new Wallet();
        senderWallet.setId(senderWalletId);
        senderWallet.setBalance(BigDecimal.valueOf(1000.00));

        receiverWallet = new Wallet();
        receiverWallet.setId(receiverWalletId);
        receiverWallet.setBalance(BigDecimal.valueOf(500.00));

        senderUser = new User();
        senderUser.setUserId("sender123");
        senderUser.setWallet(senderWallet);
        senderUser.setHashedPin("encoded123");

        receiverUser = new User();
        receiverUser.setUserId("receiver456");
        receiverUser.setWallet(receiverWallet);

        // mock security context
        when(authentication.getPrincipal()).thenReturn("sender123");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void executeP2PTransfer_insufficientFunds_throwsException() {
        BigDecimal amount = BigDecimal.valueOf(1500.00);

        when(userRepository.findById("sender123")).thenReturn(Optional.of(senderUser));
        when(userRepository.findByPhoneNumber("08123456789")).thenReturn(Optional.of(receiverUser));
        when(passwordEncoder.matches("1234", "encoded123")).thenReturn(true);
        when(feeService.calculateFee(amount)).thenReturn(BigDecimal.ZERO);
        when(walletRepository.findByIdWithLock(senderWalletId)).thenReturn(senderWallet);
        when(walletRepository.findByIdWithLock(receiverWalletId)).thenReturn(receiverWallet);

        PppsException exception = assertThrows(PppsException.class, () ->
                transferService.executeP2PTransfer("08123456789", amount, "1234", "test transfer"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Insufficient funds"));
    }

    @Test
    void executeP2PTransfer_successfulTransfer_updatesBalances() {
        BigDecimal amount = BigDecimal.valueOf(100.00);

        when(userRepository.findById("sender123")).thenReturn(Optional.of(senderUser));
        when(userRepository.findByPhoneNumber("08123456789")).thenReturn(Optional.of(receiverUser));
        when(passwordEncoder.matches("1234", "encoded123")).thenReturn(true);
        when(feeService.calculateFee(amount)).thenReturn(BigDecimal.ZERO);
        when(walletRepository.findByIdWithLock(senderWalletId)).thenReturn(senderWallet);
        when(walletRepository.findByIdWithLock(receiverWalletId)).thenReturn(receiverWallet);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(gatewayService.processPayment(any())).thenReturn(
                GatewayResponse.builder()
                        .status("SUCCESS")
                        .gatewayReference("REF123")
                        .message("Payment successful")
                        .build()
        );

        transferService.executeP2PTransfer("08123456789", amount, "1234", "test transfer");

        // ✅ assertions
        assertEquals(BigDecimal.valueOf(900.00), senderWallet.getBalance());
        assertEquals(BigDecimal.valueOf(600.00), receiverWallet.getBalance());
        verify(walletRepository, times(3)).save(any(Wallet.class)); // sender, receiver, maybe fee wallet
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // created + updated
        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class)); // debit + credit
    }
}
