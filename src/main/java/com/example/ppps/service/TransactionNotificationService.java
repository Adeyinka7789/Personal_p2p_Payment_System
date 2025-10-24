package com.example.ppps.service;

import com.example.ppps.event.TransactionCompletedEvent;
import com.example.ppps.config.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionNotificationService {

    @KafkaListener(topics = KafkaTopics.TRANSACTIONS_COMPLETED, groupId = "notification-service")
    public void handleTransactionEvent(TransactionCompletedEvent event) {
        log.info("📩 Received TransactionCompletedEvent: {}", event);

        // Example: send SMS
        sendSms(event);

        // Example: send email
        sendEmail(event);
    }

    private void sendSms(TransactionCompletedEvent event) {
        // Integrate with SMS provider (Twilio, etc.)
        log.info("Sending SMS to senderWallet: {}, receiverWallet: {}",
                event.getSenderWalletId(), event.getReceiverWalletId());
    }

    private void sendEmail(TransactionCompletedEvent event) {
        // Integrate with email service (SendGrid, etc.)
        log.info("Sending Email for transaction: {}", event.getTransactionId());
    }
}
