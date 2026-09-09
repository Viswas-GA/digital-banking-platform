package com.banking.transaction.messaging;

import com.banking.transaction.domain.Transaction;

public record TransferCompletedEvent(
        String eventType,
        Transaction transaction,
        String actorEmail,
        String actorRole
) {

    public static TransferCompletedEvent immediate(Transaction transaction, String actorEmail, String actorRole) {
        return new TransferCompletedEvent(
                com.banking.common.event.BankingEventTypes.TRANSFER_COMPLETED,
                transaction,
                actorEmail,
                actorRole
        );
    }

    public static TransferCompletedEvent scheduled(Transaction transaction) {
        return new TransferCompletedEvent(
                com.banking.common.event.BankingEventTypes.SCHEDULED_TRANSFER_COMPLETED,
                transaction,
                "system@banking",
                "SYSTEM"
        );
    }
}
