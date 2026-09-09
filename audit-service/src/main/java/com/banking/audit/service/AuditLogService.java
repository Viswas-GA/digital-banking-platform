package com.banking.audit.service;

import com.banking.audit.domain.AuditLog;
import com.banking.audit.dto.AuditLogPageResponse;
import com.banking.audit.dto.AuditLogResponse;
import com.banking.audit.repository.AuditLogRepository;
import com.banking.common.event.AuditLogPayload;
import com.banking.common.event.BankingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(BankingEvent event) {
        if (auditLogRepository.existsByEventId(event.eventId())) {
            log.debug("Skipping duplicate audit event {}", event.eventId());
            return;
        }

        AuditLogPayload payload = objectMapper.convertValue(event.payload(), AuditLogPayload.class);
        AuditLog auditLog = new AuditLog();
        auditLog.setEventId(event.eventId());
        auditLog.setActorUserId(payload.actorUserId());
        auditLog.setActorEmail(payload.actorEmail());
        auditLog.setActorRole(payload.actorRole());
        auditLog.setAction(payload.action());
        auditLog.setResourceType(payload.resourceType());
        auditLog.setResourceId(payload.resourceId());
        auditLog.setDetails(payload.details());

        auditLogRepository.save(auditLog);
        log.info("Stored audit log action={} actor={}", payload.action(), payload.actorEmail());
    }

    public AuditLogPageResponse search(String action, UUID actorUserId, int page, int size) {
        Page<AuditLog> result = auditLogRepository.search(
                action,
                actorUserId,
                PageRequest.of(page, size)
        );
        return new AuditLogPageResponse(
                result.getContent().stream().map(AuditLogService::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    static AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorUserId(),
                auditLog.getActorEmail(),
                auditLog.getActorRole(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}
