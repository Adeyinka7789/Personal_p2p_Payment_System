package com.example.ppps.service;

import com.example.ppps.entity.AuditLog;
import com.example.ppps.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void recordAction(String actorId, String action, String entityName, String entityId, String details) {
        AuditLog log = AuditLog.builder()
                .actorId(actorId)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .details(details)
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(log);
    }
}
