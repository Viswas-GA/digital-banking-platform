package com.banking.common.event;

import java.time.Instant;
import java.util.UUID;

public record BankingEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Object payload
) {

    public static BankingEvent of(String eventType, Object payload) {
        return new BankingEvent(UUID.randomUUID(), eventType, Instant.now(), payload);
    }
}
