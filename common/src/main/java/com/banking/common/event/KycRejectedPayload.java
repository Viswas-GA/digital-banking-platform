package com.banking.common.event;

import java.util.UUID;

public record KycRejectedPayload(
        UUID userId,
        String email,
        String reason
) {
}
