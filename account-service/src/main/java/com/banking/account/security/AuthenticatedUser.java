package com.banking.account.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        String role,
        String kycStatus
) {
    public boolean isKycVerified() {
        return "VERIFIED".equals(kycStatus);
    }
}
