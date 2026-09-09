package com.banking.notification.service;

import com.banking.common.event.BankingEvent;
import com.banking.common.event.BankingKafkaTopics;
import com.banking.notification.config.KafkaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class BankingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BankingEventConsumer.class);

    private final NotificationEventProcessor notificationEventProcessor;
    private final ObjectMapper objectMapper;
    private final KafkaProperties kafkaProperties;

    public BankingEventConsumer(
            NotificationEventProcessor notificationEventProcessor,
            ObjectMapper objectMapper,
            KafkaProperties kafkaProperties
    ) {
        this.notificationEventProcessor = notificationEventProcessor;
        this.objectMapper = objectMapper;
        this.kafkaProperties = kafkaProperties;
    }

    @KafkaListener(topics = BankingKafkaTopics.AUTH_EVENTS, groupId = "notification-service")
    public void consumeAuthEvent(String message) {
        consume(message);
    }

    @KafkaListener(topics = BankingKafkaTopics.TRANSACTION_EVENTS, groupId = "notification-service")
    public void consumeTransactionEvent(String message) {
        consume(message);
    }

    private void consume(String message) {
        if (!kafkaProperties.enabled()) {
            return;
        }
        try {
            BankingEvent event = objectMapper.readValue(message, BankingEvent.class);
            notificationEventProcessor.process(event);
        } catch (Exception ex) {
            log.error("Failed to process Kafka message: {}", message, ex);
            throw new IllegalStateException("Failed to process Kafka message", ex);
        }
    }
}
