package com.banking.transaction.controller;

import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferLimitResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Money transfers and transaction history")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    @SecurityRequirements
    public Map<String, String> health() {
        return Map.of(
                "service", "transaction-service",
                "status", "UP"
        );
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer money", description = "Transfer between accounts and record transaction history")
    public TransactionResponse transfer(@Valid @RequestBody TransferRequest request) {
        return transactionService.transfer(request);
    }

    @GetMapping
    @Operation(summary = "Transaction history", description = "List all transfers made by the authenticated user")
    public List<TransactionResponse> history() {
        return transactionService.getMyTransactions();
    }

    @GetMapping("/limits")
    @Operation(summary = "Daily transfer limit", description = "Shows daily limit, amount used today, and remaining")
    public TransferLimitResponse limits() {
        return transactionService.getTransferLimit();
    }
}
