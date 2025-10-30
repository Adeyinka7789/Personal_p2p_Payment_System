package com.example.ppps.event;

import com.example.ppps.config.KafkaTopics;
import com.example.ppps.event.TransactionCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionEventListener {

    @KafkaListener(topics = KafkaTopics.TRANSACTIONS_COMPLETED, groupId = "ppps-group")
    public void handleTransactionEvent(TransactionCompletedEvent event) {
        log.info("📥 Received Transaction Event: {}", event); // that's it, emoji: windows + .(dot)
    }
}
