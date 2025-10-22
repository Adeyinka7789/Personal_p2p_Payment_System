package com.example.ppps.service;

import com.example.ppps.controller.TransactionHistoryResponse;
import com.example.ppps.controller.TransactionSearchRequest;
import com.example.ppps.entity.Transaction;
import com.example.ppps.entity.TransactionStatus;
import com.example.ppps.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TransactionHistoryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionHistoryService transactionHistoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getTransactionsForWallet_withFiltersAndPagination_returnsFilteredList() {
        UUID walletId = UUID.randomUUID();
        TransactionSearchRequest filters = new TransactionSearchRequest();
        filters.setStartDate(Instant.now().minusSeconds(86400));
        filters.setPageNumber(0);
        filters.setPageSize(10);

        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setSenderWalletId(walletId);
        transaction.setAmount(BigDecimal.TEN);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setInitiatedAt(Instant.now());
        Page<Transaction> page = new PageImpl<>(Collections.singletonList(transaction));

        when(transactionRepository.findByWalletIdWithFilters(any(UUID.class), any(Instant.class), any(Instant.class),
                any(String.class), any(BigDecimal.class), any(BigDecimal.class), any(PageRequest.class)))
                .thenReturn(page);

        List<TransactionHistoryResponse> result = transactionHistoryService.getTransactionsForWallet(walletId, filters);

        assertEquals(1, result.size());
        assertEquals(transaction.getId(), result.get(0).getTransactionId());
    }
}