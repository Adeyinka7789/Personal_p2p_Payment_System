package com.example.ppps.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@Table(name = "wallets", indexes = {
        @Index(name = "idx_user_id", columnList = "userId")
})
public class Wallet {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private String currency;

    @Version
    private Long version;

    // Default constructor (required by JPA)
    public Wallet() {
        this.balance = BigDecimal.ZERO;
        this.currency = "USD"; // Default currency assumption
    }
}