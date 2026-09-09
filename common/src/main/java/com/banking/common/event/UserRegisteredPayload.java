package com.banking.common.event;

import java.util.UUID;

public record UserRegisteredPayload(
        UUID userId,
        String email,
        String firstName,
        String lastName
) {
}
