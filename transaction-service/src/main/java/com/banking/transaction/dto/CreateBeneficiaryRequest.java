package com.banking.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBeneficiaryRequest(
        @NotBlank @Size(max = 100) String nickname,
        @NotBlank @Size(max = 20) String accountNumber,
        @NotBlank @Size(max = 200) String accountHolderName
) {
}
