package com.banking.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KycRejectRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
