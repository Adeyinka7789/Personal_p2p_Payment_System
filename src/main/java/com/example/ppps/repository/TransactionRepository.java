package com.example.ppps.repository;

import com.example.ppps.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    @Query("SELECT t FROM Transaction t WHERE (t.senderWalletId = :walletId OR t.receiverWalletId = :walletId) " +
            "AND (:startDate IS NULL OR t.initiatedAt >= :startDate) " +
            "AND (:endDate IS NULL OR t.initiatedAt <= :endDate) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:minAmount IS NULL OR t.amount >= :minAmount) " +  // FIXED: t.amount not t.minAmount
            "AND (:maxAmount IS NULL OR t.amount <= :maxAmount)")
    Page<Transaction> findByWalletIdWithFilters(
            @Param("walletId") UUID walletId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("status") String status,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);
}