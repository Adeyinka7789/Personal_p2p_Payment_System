package com.example.ppps.service;

import com.example.ppps.dto.GatewayRequest;
import com.example.ppps.dto.GatewayResponse;
import com.example.ppps.exception.GatewayException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class GatewayService {

    private static final String SERVICE_NAME = "gatewayService";
    private final Random random = new Random();

    @Retry(name = SERVICE_NAME)
    @CircuitBreaker(name = SERVICE_NAME, fallbackMethod = "fallbackResponse")
    public GatewayResponse processPayment(GatewayRequest request) {
        String correlationId = MDC.get("correlationId");
        long start = System.nanoTime();

        log.info("[{}] 🌍 Initiating gateway call for transaction {}", correlationId, request.getTransactionId());

        try {
            // Simulated delay (1 sec)
            Thread.sleep(1000);

            // Simulated random failure (20% chance)
            //if (random.nextInt(10) < 2) {
                //throw new GatewayException("Simulated gateway failure");
            //}
            boolean simulateFailure = false;
            if (simulateFailure && random.nextInt(10) < 2){
                throw new GatewayException("Simulated Gateway Failure");
            }

            String reference = UUID.randomUUID().toString();
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            log.info("[{}] ✅ Gateway SUCCESS - Ref={} | Duration={}ms", correlationId, reference, durationMs);

            return GatewayResponse.builder()
                    .status("SUCCESS")
                    .gatewayReference(reference)
                    .message("Payment processed successfully (simulated)")
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GatewayException("Gateway call interrupted");
        }
    }

    // inside com.example.ppps.service.GatewayService (add method to existing class)
    @Retry(name = SERVICE_NAME)
    @CircuitBreaker(name = SERVICE_NAME, fallbackMethod = "fallbackWithdrawalResponse")
    public GatewayResponse processWithdrawal(GatewayRequest request) {
        String correlationId = MDC.get("correlationId");
        long start = System.nanoTime();
        log.info("[{}] 🌍 Initiating gateway withdrawal for transaction {}", correlationId, request.getTransactionId());

        try {
            Thread.sleep(1000);

            // simulate random failure
            if (random.nextInt(10) < 2) {
                throw new GatewayException("Simulated gateway withdrawal failure");
            }

            String reference = UUID.randomUUID().toString();
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            log.info("[{}] ✅ Gateway WITHDRAWAL SUCCESS - Ref={} | Duration={}ms", correlationId, reference, durationMs);

            return GatewayResponse.builder()
                    .status("SUCCESS")
                    .gatewayReference(reference)
                    .message("Withdrawal processed successfully (simulated)")
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GatewayException("Gateway withdrawal interrupted");
        }
    }

    // 🔄 Fallback in case of failure
    public GatewayResponse fallbackResponse(GatewayRequest request, Throwable throwable) {
        String correlationId = MDC.get("correlationId");

        log.warn("[{}] ⚠️ Gateway fallback for txn {} | Error: {} - {}",
                correlationId,
                request.getTransactionId(),
                throwable.getClass().getSimpleName(),
                throwable.getMessage());

        return GatewayResponse.builder()
                .status("PENDING")
                .gatewayReference("FALLBACK-" + UUID.randomUUID())
                .message("Gateway temporarily unavailable: " + throwable.getMessage())
                .build();
    }
}