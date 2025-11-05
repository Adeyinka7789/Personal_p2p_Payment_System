// REVIEW: full code review requested

package com.example.ppps.service;

import com.example.ppps.event.TransactionCompletedEvent;
import com.example.ppps.event.WithdrawalCompletedEvent;
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
        sendSms(event);
        sendEmail(event);
    }

    private void sendSms(TransactionCompletedEvent event) {
        // please integrate with SMS provider (Twilio etc.)
        log.info("Sending SMS to senderWallet: {}, receiverWallet: {}",
                event.getSenderWalletId(), event.getReceiverWalletId());
    }

    private void sendEmail(TransactionCompletedEvent event) {
        // please integrate with email service (SendGrid etc.)
        log.info("Sending Email for transaction: {}", event.getTransactionId());
    }

    @KafkaListener(topics = KafkaTopics.WITHDRAWAL_COMPLETED, groupId = "notification-service")
    public void handleWithdrawalEvent(WithdrawalCompletedEvent event) {
        log.info("📩 Notification Service received WithdrawalCompletedEvent: {}", event);
        sendSms(event);
        sendEmail(event);
    }

    private void sendSms(WithdrawalCompletedEvent event) {
        log.info("📲 Sending SMS for withdrawal of {} to account {} ({})",
                event.getAmount(), event.getAccountNumber(), event.getBankName());
    }

    private void sendEmail(WithdrawalCompletedEvent event) {
        log.info("📧 Sending Email confirmation for withdrawal {} to {} ({})",
                event.getTransactionId(), event.getBankName(), event.getAccountNumber());
    }
}
