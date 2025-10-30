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

    @Column(nullable = false)
    private String actorId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entityName;

    @Column(nullable = false)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant timestamp = Instant.now();
}
