package com.banking.audit.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        String role,
        String kycStatus
) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
