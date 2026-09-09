package com.banking.account.controller;

import com.banking.account.dto.InternalTransferRequest;
import com.banking.account.dto.TransferResponse;
import com.banking.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/internal")
@Tag(name = "Internal", description = "Service-to-service endpoints (not for client use)")
public class InternalAccountController {

    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/transfer")
    @Operation(summary = "Internal transfer", description = "Used by transaction-service for scheduled transfers")
    @SecurityRequirements
    public TransferResponse internalTransfer(
            @RequestHeader("X-Internal-Secret") String secret,
            @Valid @RequestBody InternalTransferRequest request
    ) {
        return accountService.internalTransfer(secret, request);
    }
}
