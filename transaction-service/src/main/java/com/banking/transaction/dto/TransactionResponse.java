package com.banking.transaction.dto;

import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String reference,
        UUID fromAccountId,
        String fromAccountNumber,
        UUID toAccountId,
        String toAccountNumber,
        BigDecimal amount,
        String currency,
        TransactionType type,
        TransactionStatus status,
        String description,
        Instant createdAt
) {
}
