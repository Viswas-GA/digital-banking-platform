package com.banking.account.dto;

import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID userId,
        String accountNumber,
        AccountType accountType,
        String currency,
        BigDecimal balance,
        AccountStatus status,
        Instant createdAt
) {
}
