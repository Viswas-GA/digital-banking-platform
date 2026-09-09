package com.banking.transaction.messaging;

import com.banking.common.event.BankingEvent;
import com.banking.common.event.BankingEventTypes;
import com.banking.common.event.BankingKafkaTopics;
import com.banking.common.event.ScheduledTransferFailedPayload;
import com.banking.common.event.TransferCompletedPayload;
import com.banking.transaction.domain.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TransactionEventPublisher {

    private final KafkaEventPublisher kafkaEventPublisher;

    public TransactionEventPublisher(KafkaEventPublisher kafkaEventPublisher) {
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferCompleted(TransferCompletedEvent event) {
        publishTransferEvent(event.eventType(), event.transaction());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScheduledTransferFailed(ScheduledTransferFailedEvent event) {
        kafkaEventPublisher.publish(
                BankingKafkaTopics.TRANSACTION_EVENTS,
                event.userId(),
                BankingEvent.of(
                        BankingEventTypes.SCHEDULED_TRANSFER_FAILED,
                        new ScheduledTransferFailedPayload(
                                event.userId(),
                                event.scheduledTransferId(),
                                event.amount(),
                                event.toAccountNumber(),
                                event.failureReason()
                        )
                )
        );
    }

    private void publishTransferEvent(String eventType, Transaction transaction) {
        kafkaEventPublisher.publish(
                BankingKafkaTopics.TRANSACTION_EVENTS,
                transaction.getUserId(),
                BankingEvent.of(
                        eventType,
                        new TransferCompletedPayload(
                                transaction.getUserId(),
                                transaction.getId(),
                                transaction.getReference(),
                                transaction.getAmount(),
                                transaction.getCurrency(),
                                transaction.getFromAccountNumber(),
                                transaction.getToAccountNumber(),
                                transaction.getDescription()
                        )
                )
        );
    }
}
