package com.example.ppps.controller;

import com.example.ppps.dto.WebhookDepositRequest;
import com.example.ppps.dto.WebhookWithdrawalRequest;
import com.example.ppps.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> handleDepositWebhook(
            @RequestHeader("X-Webhook-Signature") String signature,
            @Valid @RequestBody WebhookDepositRequest request) {

        log.info("💰 Deposit webhook received - Reference: {}, Amount: {}",
                request.getExternalReference(), request.getAmount());

        try {
            webhookService.processDepositWebhook(request, signature);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Deposit webhook processed successfully"
            ));
                // note: emojis are intentional windows + .(dot)
        } catch (Exception e) {
            log.error("❌ Deposit webhook processing failed - Reference: {}",
                    request.getExternalReference(), e);

            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/withdrawal")
    public ResponseEntity<Map<String, Object>> handleWithdrawalWebhook(
            @RequestHeader("X-Webhook-Signature") String signature,
            @Valid @RequestBody WebhookWithdrawalRequest request) {

        log.info("💸 Withdrawal webhook received - Reference: {}, Status: {}",
                request.getExternalReference(), request.getStatus());

        try {
            webhookService.processWithdrawalWebhook(request, signature);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Withdrawal webhook processed successfully"
            ));

        } catch (Exception e) {
            log.error("❌ Withdrawal webhook processing failed - Reference: {}",
                    request.getExternalReference(), e);

            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    //health check for gateway providers
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "ppps-webhooks",
                "timestamp", System.currentTimeMillis()
        ));
    }
}