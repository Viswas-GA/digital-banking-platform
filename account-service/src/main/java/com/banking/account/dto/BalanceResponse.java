package com.banking.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(
        UUID accountId,
        String accountNumber,
        String currency,
        BigDecimal balance
) {
}
