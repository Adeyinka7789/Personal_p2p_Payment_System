package com.example.ppps.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    // Who performed the action (e.g., admin user ID, service name)
    @Column(nullable = false)
    private String actorId;

    // What action they took (CREATE_USER, UPDATE_WALLET, DELETE_TRANSACTION, etc.)
    @Column(nullable = false)
    private String action;

    // The name of the entity affected (e.g., "Wallet", "User", "Transaction")
    @Column(nullable = false)
    private String entityName;

    // The ID of that entity (as string to support multiple ID types)
    @Column(nullable = false)
    private String entityId;

    // Additional context or details about what was changed
    @Column(columnDefinition = "TEXT")
    private String details;

    // Timestamp when it occurred
    @Column(nullable = false, updatable = false)
    private Instant timestamp = Instant.now();
}
