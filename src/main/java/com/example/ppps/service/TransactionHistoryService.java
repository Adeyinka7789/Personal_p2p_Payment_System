package com.example.ppps.service;

import com.example.ppps.controller.TransactionHistoryResponse;
import com.example.ppps.controller.TransactionSearchRequest;
import com.example.ppps.entity.Transaction;
import com.example.ppps.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TransactionHistoryService {

    @Autowired
    private TransactionRepository transactionRepository;

    public List<TransactionHistoryResponse> getTransactionsForWallet(UUID walletId, TransactionSearchRequest filters) {
        int pageNumber = filters.getPageNumber() != null ? filters.getPageNumber() : 0;
        int pageSize = filters.getPageSize() != null ? filters.getPageSize() : 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Transaction> transactionPage = transactionRepository.findByWalletIdWithFilters(
                walletId,
                filters.getStartDate(),
                filters.getEndDate(),
                filters.getStatus(),
                filters.getMinAmount(),
                filters.getMaxAmount(),
                pageable
        );

        return transactionPage.getContent().stream()
                .map(t -> new TransactionHistoryResponse(
                        t.getId(),
                        t.getSenderWalletId(),
                        t.getReceiverWalletId(),
                        t.getAmount(),
                        t.getStatus().name(),
                        t.getInitiatedAt()
                ))
                .toList();  // Changed from .collect(Collectors.toList()) - Java 17 feature
    }
}