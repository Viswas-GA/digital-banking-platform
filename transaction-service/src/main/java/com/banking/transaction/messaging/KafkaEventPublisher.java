package com.banking.transaction.messaging;

import com.banking.transaction.config.KafkaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaProperties kafkaProperties;

    public KafkaEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            KafkaProperties kafkaProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.kafkaProperties = kafkaProperties;
    }

    public void publish(String topic, UUID userId, Object event) {
        if (!kafkaProperties.enabled()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, userId.toString(), payload);
            log.info("Published Kafka event to {} for user {}", topic, userId);
        } catch (Exception ex) {
            log.error("Failed to publish Kafka event to {} for user {}", topic, userId, ex);
        }
    }
}
