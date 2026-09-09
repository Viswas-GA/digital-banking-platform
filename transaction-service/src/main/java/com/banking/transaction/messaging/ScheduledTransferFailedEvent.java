package com.banking.transaction.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record ScheduledTransferFailedEvent(
        UUID userId,
        UUID scheduledTransferId,
        BigDecimal amount,
        String toAccountNumber,
        String failureReason
) {
}
