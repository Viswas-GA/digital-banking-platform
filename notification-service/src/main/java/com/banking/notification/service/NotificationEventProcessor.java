package com.banking.notification.service;

import com.banking.common.event.BankingEvent;
import com.banking.common.event.BankingEventTypes;
import com.banking.common.event.KycApprovedPayload;
import com.banking.common.event.KycRejectedPayload;
import com.banking.common.event.ScheduledTransferFailedPayload;
import com.banking.common.event.TransferCompletedPayload;
import com.banking.common.event.UserRegisteredPayload;
import com.banking.notification.domain.Notification;
import com.banking.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventProcessor.class);

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public NotificationEventProcessor(NotificationRepository notificationRepository, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(BankingEvent event) {
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.debug("Skipping duplicate event {}", event.eventId());
            return;
        }

        Notification notification = switch (event.eventType()) {
            case BankingEventTypes.USER_REGISTERED -> fromUserRegistered(event);
            case BankingEventTypes.KYC_APPROVED -> fromKycApproved(event);
            case BankingEventTypes.KYC_REJECTED -> fromKycRejected(event);
            case BankingEventTypes.TRANSFER_COMPLETED,
                 BankingEventTypes.SCHEDULED_TRANSFER_COMPLETED -> fromTransferCompleted(event);
            case BankingEventTypes.SCHEDULED_TRANSFER_FAILED -> fromScheduledTransferFailed(event);
            default -> {
                log.warn("Unsupported event type: {}", event.eventType());
                yield null;
            }
        };

        if (notification != null) {
            notificationRepository.save(notification);
            log.info("Stored notification for user {} type {}", notification.getUserId(), notification.getEventType());
        }
    }

    private Notification fromUserRegistered(BankingEvent event) {
        UserRegisteredPayload payload = convert(event.payload(), UserRegisteredPayload.class);
        Notification notification = baseNotification(event);
        notification.setUserId(payload.userId());
        notification.setTitle("Welcome to Digital Banking");
        notification.setMessage("Hi " + payload.firstName() + ", your account has been created successfully.");
        return notification;
    }

    private Notification fromKycApproved(BankingEvent event) {
        KycApprovedPayload payload = convert(event.payload(), KycApprovedPayload.class);
        Notification notification = baseNotification(event);
        notification.setUserId(payload.userId());
        notification.setTitle("KYC Verified");
        notification.setMessage("Your identity verification is complete. You can now open accounts and transfer money.");
        return notification;
    }

    private Notification fromKycRejected(BankingEvent event) {
        KycRejectedPayload payload = convert(event.payload(), KycRejectedPayload.class);
        Notification notification = baseNotification(event);
        notification.setUserId(payload.userId());
        notification.setTitle("KYC Rejected");
        notification.setMessage("Your KYC submission was rejected. Reason: " + payload.reason());
        return notification;
    }

    private Notification fromTransferCompleted(BankingEvent event) {
        TransferCompletedPayload payload = convert(event.payload(), TransferCompletedPayload.class);
        Notification notification = baseNotification(event);
        notification.setUserId(payload.userId());
        notification.setReferenceId(payload.transactionId());
        notification.setTitle("Transfer Completed");
        notification.setMessage(String.format(
                "Transfer of %s %s from %s to %s completed. Reference: %s",
                payload.amount(),
                payload.currency(),
                payload.fromAccountNumber(),
                payload.toAccountNumber(),
                payload.reference()
        ));
        return notification;
    }

    private Notification fromScheduledTransferFailed(BankingEvent event) {
        ScheduledTransferFailedPayload payload = convert(event.payload(), ScheduledTransferFailedPayload.class);
        Notification notification = baseNotification(event);
        notification.setUserId(payload.userId());
        notification.setReferenceId(payload.scheduledTransferId());
        notification.setTitle("Scheduled Transfer Failed");
        notification.setMessage(String.format(
                "Scheduled transfer of %s to %s failed. Reason: %s",
                payload.amount(),
                payload.toAccountNumber(),
                payload.failureReason()
        ));
        return notification;
    }

    private Notification baseNotification(BankingEvent event) {
        Notification notification = new Notification();
        notification.setEventId(event.eventId());
        notification.setEventType(event.eventType());
        notification.setRead(false);
        return notification;
    }

    private <T> T convert(Object payload, Class<T> type) {
        return objectMapper.convertValue(payload, type);
    }
}
