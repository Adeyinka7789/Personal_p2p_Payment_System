package com.example.ppps.service;

import com.example.ppps.exception.PppsException;
import com.example.ppps.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WithdrawalServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FeeService feeService;
    @Mock
    private GatewayService gatewayService;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private PasswordEncoder passwordEncoder;

    private WithdrawalService withdrawalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        withdrawalService = new WithdrawalService(
                walletRepository,
                transactionRepository,
                ledgerEntryRepository,
                userRepository,
                feeService,
                gatewayService,
                kafkaTemplate,
                passwordEncoder,
                UUID.randomUUID().toString()  // dummy platform wallet ID
        );
    }

    @Test
    void verifyPin_ShouldReturnTrue_WhenPinMatches() {
        // given
        String plainPin = "123456";
        String hashedPin = "$2a$10$abcdef"; // mock hash

        when(passwordEncoder.matches(plainPin, hashedPin)).thenReturn(true);

        // when
        boolean result = invokeVerifyPin(plainPin, hashedPin);

        // then
        assertTrue(result);
        verify(passwordEncoder).matches(plainPin, hashedPin);
    }

    @Test
    void verifyPin_ShouldReturnFalse_WhenPinDoesNotMatch() {
        // given
        String plainPin = "123456";
        String hashedPin = "$2a$10$abcdef";

        when(passwordEncoder.matches(plainPin, hashedPin)).thenReturn(false);

        // when
        boolean result = invokeVerifyPin(plainPin, hashedPin);

        // then
        assertFalse(result);
        verify(passwordEncoder).matches(plainPin, hashedPin);
    }

    @Test
    void verifyPin_ShouldThrowException_WhenPinIsNull() {
        // expect
        PppsException exception = assertThrows(PppsException.class,
                () -> invokeVerifyPin("123456", null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("User PIN not set", exception.getMessage());
    }

    // helper to call the private method reflectively
    private boolean invokeVerifyPin(String providedPin, String hashedPin) {
        try {
            var method = WithdrawalService.class.getDeclaredMethod("verifyPin", String.class, String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(withdrawalService, providedPin, hashedPin);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
