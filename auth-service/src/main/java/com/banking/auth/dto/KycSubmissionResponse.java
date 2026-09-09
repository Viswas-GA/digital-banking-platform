package com.banking.auth.dto;

import com.banking.auth.domain.DocumentType;
import com.banking.auth.domain.KycStatus;
import com.banking.auth.domain.KycSubmissionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record KycSubmissionResponse(
        UUID id,
        UUID userId,
        DocumentType documentType,
        String documentNumber,
        LocalDate dateOfBirth,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        KycSubmissionStatus status,
        String rejectionReason,
        Instant reviewedAt,
        UUID reviewedBy,
        Instant createdAt
) {
}
