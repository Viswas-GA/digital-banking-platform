package com.banking.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCompletedPayload(
        UUID userId,
        UUID transactionId,
        String reference,
        BigDecimal amount,
        String currency,
        String fromAccountNumber,
        String toAccountNumber,
        String description
) {
}
