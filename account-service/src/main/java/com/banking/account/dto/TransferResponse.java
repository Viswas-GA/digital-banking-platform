package com.banking.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(
        UUID fromAccountId,
        String fromAccountNumber,
        UUID toAccountId,
        String toAccountNumber,
        BigDecimal amount,
        String currency,
        BigDecimal fromAccountBalance,
        String description
) {
}
