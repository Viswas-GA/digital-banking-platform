package com.banking.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ScheduledTransferFailedPayload(
        UUID userId,
        UUID scheduledTransferId,
        BigDecimal amount,
        String toAccountNumber,
        String failureReason
) {
}
