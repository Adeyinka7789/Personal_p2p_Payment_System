package com.example.ppps.entity;

import com.example.ppps.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "transactions", indexes = {
        @Index(name = "idx_sender_wallet_id", columnList = "senderWalletId"),
        @Index(name = "idx_receiver_wallet_id", columnList = "receiverWalletId"),
        @Index(name = "idx_initiated_at", columnList = "initiatedAt")
})
public class Transaction {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID senderWalletId;

    @Column(nullable = false)
    private UUID receiverWalletId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private Instant initiatedAt;
}