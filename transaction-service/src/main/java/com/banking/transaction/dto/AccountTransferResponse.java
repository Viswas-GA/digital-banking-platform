package com.banking.transaction.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountTransferResponse(
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
