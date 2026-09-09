package com.banking.auth.dto;

import com.banking.auth.domain.KycStatus;

public record KycStatusResponse(
        KycStatus kycStatus,
        KycSubmissionResponse latestSubmission
) {
}
