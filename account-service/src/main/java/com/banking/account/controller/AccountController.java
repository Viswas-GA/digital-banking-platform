package com.banking.account.controller;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.BalanceResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.dto.TransferRequest;
import com.banking.account.dto.TransferResponse;
import com.banking.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Bank account management APIs")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    @SecurityRequirements
    public Map<String, String> health() {
        return Map.of(
                "service", "account-service",
                "status", "UP"
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create account", description = "Opens a new bank account (requires VERIFIED KYC in JWT)")
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    @GetMapping
    @Operation(summary = "List my accounts")
    public List<AccountResponse> listAccounts() {
        return accountService.listMyAccounts();
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account details")
    public AccountResponse getAccount(@PathVariable UUID accountId) {
        return accountService.getAccount(accountId);
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balance")
    public BalanceResponse getBalance(@PathVariable UUID accountId) {
        return accountService.getBalance(accountId);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money", description = "Transfer between accounts (requires VERIFIED KYC)")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return accountService.transfer(request);
    }
}
