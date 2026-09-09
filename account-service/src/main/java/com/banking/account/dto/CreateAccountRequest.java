package com.banking.account.dto;

import com.banking.account.domain.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotNull AccountType accountType,
        @Size(min = 3, max = 3) @Pattern(regexp = "[A-Z]{3}") String currency
) {
}
