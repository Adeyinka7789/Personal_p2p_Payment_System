package com.example.ppps.service;

import com.example.ppps.event.TransactionCompletedEvent;
import com.example.ppps.config.KafkaTopics;
import com.example.ppps.event.WithdrawalCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionAnalyticsService {

    @KafkaListener(topics = KafkaTopics.TRANSACTIONS_COMPLETED, groupId = "analytics-service")
    public void handleTransactionEvent(TransactionCompletedEvent event) {
        log.info("📊 Analytics received TransactionCompletedEvent: {}", event);
        storeTransactionEvent(event);
    }

    private void storeTransactionEvent(TransactionCompletedEvent event) {
        // your DB insert logic here if you need this feature
        log.info("Stored transaction {} in analytics DB", event.getTransactionId());
    }

    @KafkaListener(topics = KafkaTopics.WITHDRAWAL_COMPLETED, groupId = "analytics-service")
    public void handleWithdrawalEvent(WithdrawalCompletedEvent event) {
        log.info("📊 Analytics received WithdrawalCompletedEvent: {}", event);
        storeWithdrawalEvent(event);
    }

    private void storeWithdrawalEvent(WithdrawalCompletedEvent event) {
        log.info("Stored withdrawal {} for amount {} in analytics DB",
                event.getTransactionId(), event.getAmount());
    }
}

