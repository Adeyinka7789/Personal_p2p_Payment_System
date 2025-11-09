package com.example.ppps.service;

import com.example.ppps.exception.PppsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@Slf4j
public class WebhookSecurityService {

    @Value("${app.webhook.secret:default-webhook-secret-change-in-production}")
    private String webhookSecret;

    private static final String HMAC_SHA256 = "HmacSHA256";

    // verify webhook signature to ensure request authenticity
    public void verifySignature(Object request, String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            throw new PppsException(HttpStatus.UNAUTHORIZED, "Missing webhook signature");
        }

        try {
            String payload = request.toString(); // In practice, use the raw request body
            String expectedSignature = calculateSignature(payload);

            if (!signature.equals(expectedSignature)) {
                log.error("❌ Webhook signature verification failed");
                throw new PppsException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
            }

            log.debug("✅ Webhook signature verified successfully");

        } catch (Exception e) {
            log.error("🔐 Webhook signature verification error", e);
            throw new PppsException(HttpStatus.UNAUTHORIZED, "Signature verification failed");
        }
    }

    // let's calculate HMAC SHA256 signature for webhook verification

    private String calculateSignature(String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKeySpec);
        byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    // validate webhook payload structure
    public void validatePayload(Object request) {
        if (request == null) {
            throw new PppsException(HttpStatus.BAD_REQUEST, "Webhook payload cannot be null");
        }
        log.debug("✅ Webhook payload validation successful");
    }
}