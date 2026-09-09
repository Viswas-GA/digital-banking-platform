package com.banking.audit.service;

import com.banking.common.event.BankingEvent;
import com.banking.common.event.BankingKafkaTopics;
import com.banking.audit.config.KafkaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = BankingKafkaTopics.AUDIT_EVENTS, groupId = "audit-service")
    public void consume(String message) {
        try {
            BankingEvent event = objectMapper.readValue(message, BankingEvent.class);
            auditLogService.process(event);
        } catch (Exception ex) {
            log.error("Failed to process audit message: {}", message, ex);
            throw new IllegalStateException("Failed to process audit message", ex);
        }
    }
}
