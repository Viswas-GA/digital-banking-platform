package com.banking.transaction.dto;

import com.banking.transaction.domain.ScheduledTransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ScheduledTransferResponse(
        UUID id,
        UUID fromAccountId,
        String toAccountNumber,
        BigDecimal amount,
        String description,
        Instant scheduledAt,
        ScheduledTransferStatus status,
        String failureReason,
        UUID transactionId,
        Instant createdAt
) {
}
