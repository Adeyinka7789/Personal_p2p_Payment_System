package com.example.ppps.controller;

import com.example.ppps.entity.AuditLog;
import com.example.ppps.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public List<AuditLog> getAll() {
        return auditLogRepository.findAll();
    }
}
