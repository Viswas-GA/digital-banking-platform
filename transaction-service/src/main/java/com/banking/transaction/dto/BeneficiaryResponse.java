package com.banking.transaction.dto;

import java.time.Instant;
import java.util.UUID;

public record BeneficiaryResponse(
        UUID id,
        String nickname,
        String accountNumber,
        String accountHolderName,
        Instant createdAt
) {
}
