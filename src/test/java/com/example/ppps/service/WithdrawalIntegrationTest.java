// REVIEW: full code review requested

package com.example.ppps.service;

import com.example.ppps.dto.GatewayResponse;
import com.example.ppps.dto.WithdrawRequest;
import com.example.ppps.entity.Transaction;
import com.example.ppps.enums.TransactionStatus;
import com.example.ppps.entity.Wallet;
import com.example.ppps.repository.TransactionRepository;
import com.example.ppps.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WithdrawalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GatewayService gatewayService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Wallet wallet;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();

        wallet = new Wallet();
        wallet.setUserId(UUID.randomUUID());
        wallet.setBalance(BigDecimal.valueOf(5000));
        wallet.setCurrency("USD");
        walletRepository.save(wallet);

        when(passwordEncoder.matches("1234", "encodedPIN")).thenReturn(true);
    }

    @Test
    void testSuccessfulWithdrawalFlow() throws Exception {
        WithdrawRequest request = new WithdrawRequest();
        request.setWalletId(wallet.getUserId());
        request.setAmount(BigDecimal.valueOf(1000));
        request.setSecurePin("1234");
        request.setAccountNumber("1234567890");
        request.setBankName("001");

        when(gatewayService.processWithdrawal(any()))
                .thenReturn(GatewayResponse.builder()
                        .status("SUCCESS")
                        .gatewayReference("TXN-REF-001")
                        .message("Withdrawal processed successfully (mocked)")
                        .build());


        mockMvc.perform(post("/api/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionReference").value("TXN-REF-001"));

        Optional<Transaction> transactionOpt = transactionRepository.findAll().stream().findFirst();
        assert(transactionOpt.isPresent());
        Transaction txn = transactionOpt.get();

        Wallet updatedWallet = walletRepository.findById(wallet.getId()).get();
        assert(updatedWallet.getBalance().compareTo(BigDecimal.valueOf(4000)) == 0);

        assert(txn.getAmount().equals(BigDecimal.valueOf(1000)));
        assert(txn.getStatus() == TransactionStatus.SUCCESS);

        verify(gatewayService, times(1)).processWithdrawal(any());
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
    }

    @Test
    void testWithdrawalFails_IfInsufficientFunds() throws Exception {
        WithdrawRequest request = new WithdrawRequest();
        request.setWalletId(wallet.getUserId());
        request.setAmount(BigDecimal.valueOf(10000));
        request.setSecurePin("1234");

        mockMvc.perform(post("/api/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient balance"));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        GatewayService gatewayService() {
            return Mockito.mock(GatewayService.class);
        }

        @Bean
        KafkaTemplate<String, Object> kafkaTemplate() {
            return Mockito.mock(KafkaTemplate.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return Mockito.mock(PasswordEncoder.class);
        }
    }
}
