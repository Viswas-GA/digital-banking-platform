package com.banking.auth.messaging;

import com.banking.auth.domain.User;
import com.banking.common.event.BankingEvent;
import com.banking.common.event.BankingEventTypes;
import com.banking.common.event.BankingKafkaTopics;
import com.banking.common.event.KycApprovedPayload;
import com.banking.common.event.KycRejectedPayload;
import com.banking.common.event.UserRegisteredPayload;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AuthEventPublisher {

    private final KafkaEventPublisher kafkaEventPublisher;

    public AuthEventPublisher(KafkaEventPublisher kafkaEventPublisher) {
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        User user = event.user();
        kafkaEventPublisher.publish(
                BankingKafkaTopics.AUTH_EVENTS,
                user.getId(),
                BankingEvent.of(
                        BankingEventTypes.USER_REGISTERED,
                        new UserRegisteredPayload(
                                user.getId(),
                                user.getEmail(),
                                user.getFirstName(),
                                user.getLastName()
                        )
                )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onKycApproved(KycApprovedEvent event) {
        User user = event.user();
        kafkaEventPublisher.publish(
                BankingKafkaTopics.AUTH_EVENTS,
                user.getId(),
                BankingEvent.of(
                        BankingEventTypes.KYC_APPROVED,
                        new KycApprovedPayload(user.getId(), user.getEmail())
                )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onKycRejected(KycRejectedEvent event) {
        User user = event.user();
        kafkaEventPublisher.publish(
                BankingKafkaTopics.AUTH_EVENTS,
                user.getId(),
                BankingEvent.of(
                        BankingEventTypes.KYC_REJECTED,
                        new KycRejectedPayload(user.getId(), user.getEmail(), event.reason())
                )
        );
    }
}
