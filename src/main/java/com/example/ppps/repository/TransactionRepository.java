package com.example.ppps.repository;

import com.example.ppps.entity.Transaction;
import com.example.ppps.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByStatus(TransactionStatus status);

    // find transactions by wallet ID with optional filters
    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.senderWalletId = :walletId OR t.receiverWalletId = :walletId) " +
            "AND t.initiatedAt >= COALESCE(:startDate, t.initiatedAt) " +
            "AND t.initiatedAt <= COALESCE(:endDate, t.initiatedAt) " +
            "AND (COALESCE(:status, '') = '' OR CAST(t.status AS string) = :status) " +
            "AND t.amount >= COALESCE(:minAmount, t.amount) " +
            "AND t.amount <= COALESCE(:maxAmount, t.amount) " +
            "ORDER BY t.initiatedAt DESC")
    Page<Transaction> findByWalletIdWithFilters(
            @Param("walletId") UUID walletId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("status") String status,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);

    //find all transactions for a wallet without filters
    @Query("SELECT t FROM Transaction t WHERE t.senderWalletId = :walletId OR t.receiverWalletId = :walletId ORDER BY t.initiatedAt DESC")
    Page<Transaction> findByWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    //find pending transactions initiated before a certain time
    List<Transaction> findByStatusAndInitiatedAtBefore(
            @Param("status") TransactionStatus status,
            @Param("initiatedAt") Instant initiatedAt
    );
}