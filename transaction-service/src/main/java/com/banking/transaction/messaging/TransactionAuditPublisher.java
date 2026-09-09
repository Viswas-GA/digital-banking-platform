package com.banking.transaction.messaging;

import com.banking.common.event.AuditActions;
import com.banking.common.event.AuditLogPayload;
import com.banking.common.event.BankingEvent;
import com.banking.common.event.BankingEventTypes;
import com.banking.common.event.BankingKafkaTopics;
import com.banking.transaction.domain.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TransactionAuditPublisher {

    private final KafkaEventPublisher kafkaEventPublisher;

    public TransactionAuditPublisher(KafkaEventPublisher kafkaEventPublisher) {
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferCompleted(TransferCompletedEvent event) {
        Transaction transaction = event.transaction();
        String action = BankingEventTypes.TRANSFER_COMPLETED.equals(event.eventType())
                ? AuditActions.TRANSFER_COMPLETED
                : AuditActions.SCHEDULED_TRANSFER_COMPLETED;

        publish(new AuditLogPayload(
                transaction.getUserId(),
                event.actorEmail(),
                event.actorRole(),
                action,
                "TRANSACTION",
                transaction.getId().toString(),
                "Transfer " + transaction.getReference() + " amount " + transaction.getAmount()
                        + " " + transaction.getCurrency() + " from " + transaction.getFromAccountNumber()
                        + " to " + transaction.getToAccountNumber()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScheduledTransferFailed(ScheduledTransferFailedEvent event) {
        publish(new AuditLogPayload(
                event.userId(),
                "system@banking",
                "SYSTEM",
                AuditActions.SCHEDULED_TRANSFER_FAILED,
                "SCHEDULED_TRANSFER",
                event.scheduledTransferId().toString(),
                "Scheduled transfer of " + event.amount() + " to " + event.toAccountNumber()
                        + " failed: " + event.failureReason()
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
