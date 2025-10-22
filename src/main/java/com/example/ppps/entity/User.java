package com.example.ppps.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String userId;

    @PrePersist
    public void generateId() {
        if (this.userId == null) {
            this.userId = UUID.randomUUID().toString();
        }
    }

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private String hashedPin;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "wallet_id", referencedColumnName = "id")
    private Wallet wallet;
}