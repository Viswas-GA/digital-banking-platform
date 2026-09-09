package com.banking.transaction.controller;

import com.banking.transaction.dto.BeneficiaryResponse;
import com.banking.transaction.dto.CreateBeneficiaryRequest;
import com.banking.transaction.service.BeneficiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/beneficiaries")
@Tag(name = "Beneficiaries", description = "Saved transfer recipients")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add beneficiary")
    public BeneficiaryResponse addBeneficiary(@Valid @RequestBody CreateBeneficiaryRequest request) {
        return beneficiaryService.addBeneficiary(request);
    }

    @GetMapping
    @Operation(summary = "List beneficiaries")
    public List<BeneficiaryResponse> listBeneficiaries() {
        return beneficiaryService.listBeneficiaries();
    }

    @DeleteMapping("/{beneficiaryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete beneficiary")
    public void deleteBeneficiary(@PathVariable UUID beneficiaryId) {
        beneficiaryService.deleteBeneficiary(beneficiaryId);
    }
}
