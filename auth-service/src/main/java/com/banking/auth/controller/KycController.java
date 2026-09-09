package com.banking.auth.controller;

import com.banking.auth.dto.KycRejectRequest;
import com.banking.auth.dto.KycStatusResponse;
import com.banking.auth.dto.KycSubmissionResponse;
import com.banking.auth.dto.KycSubmitRequest;
import com.banking.auth.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kyc")
@Tag(name = "KYC", description = "Know Your Customer submission and review APIs")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit KYC", description = "Submit identity details for verification (requires JWT)")
    public KycSubmissionResponse submit(@Valid @RequestBody KycSubmitRequest request) {
        return kycService.submit(request);
    }

    @GetMapping("/status")
    @Operation(summary = "Get KYC status", description = "Returns current KYC status and latest submission (requires JWT)")
    public KycStatusResponse status() {
        return kycService.getStatus();
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List pending KYC submissions", description = "Admin only - lists submissions awaiting review")
    public List<KycSubmissionResponse> pendingSubmissions() {
        return kycService.listPendingSubmissions();
    }

    @PostMapping("/admin/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve KYC", description = "Admin only - approves a user's pending KYC submission")
    public KycSubmissionResponse approve(@PathVariable UUID userId) {
        return kycService.approve(userId);
    }

    @PostMapping("/admin/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject KYC", description = "Admin only - rejects a user's pending KYC submission")
    public KycSubmissionResponse reject(@PathVariable UUID userId, @Valid @RequestBody KycRejectRequest request) {
        return kycService.reject(userId, request);
    }
}
