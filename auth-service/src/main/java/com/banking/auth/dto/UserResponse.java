package com.banking.auth.dto;

import com.banking.auth.domain.KycStatus;
import com.banking.auth.domain.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        KycStatus kycStatus
) {
}
