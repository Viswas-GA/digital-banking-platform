package com.banking.audit.service;

import com.banking.common.event.AuditActions;
import com.banking.common.event.AuditLogPayload;
import com.banking.common.event.BankingEvent;
import com.banking.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void processesAuditEvent() {
        when(auditLogRepository.existsByEventId(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        auditLogService = new AuditLogService(auditLogRepository, objectMapper);
        auditLogService.process(BankingEvent.of(
                AuditActions.KYC_APPROVED,
                new AuditLogPayload(
                        UUID.randomUUID(),
                        "admin@example.com",
                        "ADMIN",
                        AuditActions.KYC_APPROVED,
                        "KYC",
                        UUID.randomUUID().toString(),
                        "Approved KYC"
                )
        ));

        verify(auditLogRepository).save(any());
    }

    @Test
    void skipsDuplicateEvents() {
        UUID eventId = UUID.randomUUID();
        when(auditLogRepository.existsByEventId(eventId)).thenReturn(true);

        auditLogService = new AuditLogService(auditLogRepository, objectMapper);
        auditLogService.process(new BankingEvent(
                eventId,
                AuditActions.USER_REGISTERED,
                java.time.Instant.now(),
                new AuditLogPayload(
                        UUID.randomUUID(), "u@e.com", "USER",
                        AuditActions.USER_REGISTERED, "USER", "id", "details"
                )
        ));

        verify(auditLogRepository, never()).save(any());
    }
}
