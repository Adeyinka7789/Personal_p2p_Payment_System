package com.example.ppps.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType;

    @Column(nullable = false, columnDefinition = "Decimal(15,2)")
    private BigDecimal amount;

    private Instant createdAt;

    // ✅ Default constructor for JPA
    public LedgerEntry() {}

    // ✅ Custom constructor for convenient instantiation
    public LedgerEntry(UUID id, BigDecimal amount, Instant createdAt,
                       EntryType entryType, UUID transactionId, UUID walletId) {
        this.id = id;
        this.amount = amount;
        this.createdAt = createdAt;
        this.entryType = entryType;
        this.transactionId = transactionId;
        this.walletId = walletId;
    }
}