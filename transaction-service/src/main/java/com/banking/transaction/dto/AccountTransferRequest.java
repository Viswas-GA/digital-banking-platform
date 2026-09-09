package com.banking.transaction.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountTransferRequest(
        UUID fromAccountId,
        String toAccountNumber,
        BigDecimal amount,
        String description
) {
}
