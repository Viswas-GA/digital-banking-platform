package com.banking.auth.messaging;

import com.banking.auth.domain.User;
import com.banking.common.event.AuditActions;
import com.banking.common.event.AuditLogPayload;
import com.banking.common.event.BankingEvent;
import com.banking.common.event.BankingKafkaTopics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AuthAuditPublisher {

    private final KafkaEventPublisher kafkaEventPublisher;

    public AuthAuditPublisher(KafkaEventPublisher kafkaEventPublisher) {
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        User user = event.user();
        publish(new AuditLogPayload(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                AuditActions.USER_REGISTERED,
                "USER",
                user.getId().toString(),
                "New user registration"
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onKycSubmitted(KycSubmittedEvent event) {
        User user = event.user();
        publish(new AuditLogPayload(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                AuditActions.KYC_SUBMITTED,
                "KYC",
                event.submissionId().toString(),
                "KYC documents submitted for review"
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onKycApproved(KycApprovedEvent event) {
        User admin = event.admin();
        User user = event.user();
        publish(new AuditLogPayload(
                admin.getId(),
                admin.getEmail(),
                admin.getRole().name(),
                AuditActions.KYC_APPROVED,
                "KYC",
                user.getId().toString(),
                "KYC approved for user " + user.getEmail()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onKycRejected(KycRejectedEvent event) {
        User admin = event.admin();
        User user = event.user();
        publish(new AuditLogPayload(
                admin.getId(),
                admin.getEmail(),
                admin.getRole().name(),
                AuditActions.KYC_REJECTED,
                "KYC",
                user.getId().toString(),
                "KYC rejected for user " + user.getEmail() + ". Reason: " + event.reason()
        ));
    }

    private void publish(AuditLogPayload payload) {
        kafkaEventPublisher.publish(
                BankingKafkaTopics.AUDIT_EVENTS,
                payload.actorUserId(),
                BankingEvent.of(payload.action(), payload)
        );
    }
}
