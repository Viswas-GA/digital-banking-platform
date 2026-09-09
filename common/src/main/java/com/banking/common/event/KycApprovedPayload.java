package com.banking.common.event;

import java.util.UUID;

public record KycApprovedPayload(
        UUID userId,
        String email
) {
}
