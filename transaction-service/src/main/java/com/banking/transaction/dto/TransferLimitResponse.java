package com.banking.transaction.dto;

import java.math.BigDecimal;

public record TransferLimitResponse(
        BigDecimal dailyLimit,
        BigDecimal usedToday,
        BigDecimal remaining
) {
}
