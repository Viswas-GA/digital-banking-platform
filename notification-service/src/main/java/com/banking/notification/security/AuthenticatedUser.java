package com.banking.notification.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        String role,
        String kycStatus
) {
}
