package com.banking.notification.service;

import com.banking.common.event.BankingEvent;
import com.banking.common.event.BankingEventTypes;
import com.banking.common.event.KycApprovedPayload;
import com.banking.common.event.UserRegisteredPayload;
import com.banking.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventProcessorTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationEventProcessor notificationEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void processesUserRegisteredEvent() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(any())).thenReturn(false);

        notificationEventProcessor = new NotificationEventProcessor(notificationRepository, objectMapper);
        notificationEventProcessor.process(BankingEvent.of(
                BankingEventTypes.USER_REGISTERED,
                new UserRegisteredPayload(userId, "user@example.com", "Alice", "Smith")
        ));

        verify(notificationRepository).save(any());
    }

    @Test
    void skipsDuplicateEvents() {
        UUID eventId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(true);

        notificationEventProcessor = new NotificationEventProcessor(notificationRepository, objectMapper);
        notificationEventProcessor.process(new BankingEvent(
                eventId,
                BankingEventTypes.KYC_APPROVED,
                java.time.Instant.now(),
                new KycApprovedPayload(UUID.randomUUID(), "user@example.com")
        ));

        verify(notificationRepository, never()).save(any());
    }
}
