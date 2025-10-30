package com.example.ppps.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(nullable = true, unique = true)
    private String email;

    @Column(nullable = false)
    private String hashedPin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'USER'")
    private UserRole role = UserRole.USER; // Default to USER

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "wallet_id", referencedColumnName = "id")
    @JsonIgnore
    private Wallet wallet;

    public enum UserRole {
        USER,
        ADMIN
    }
}