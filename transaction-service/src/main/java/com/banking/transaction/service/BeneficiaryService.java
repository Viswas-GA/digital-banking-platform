package com.banking.transaction.service;

import com.banking.transaction.domain.Beneficiary;
import com.banking.transaction.dto.BeneficiaryResponse;
import com.banking.transaction.dto.CreateBeneficiaryRequest;
import com.banking.transaction.repository.BeneficiaryRepository;
import com.banking.transaction.security.AuthenticatedUser;
import com.banking.common.exception.BankingException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AuthContextService authContextService;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository, AuthContextService authContextService) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.authContextService = authContextService;
    }

    @Transactional
    public BeneficiaryResponse addBeneficiary(CreateBeneficiaryRequest request) {
        AuthenticatedUser user = authContextService.getAuthenticatedUser();

        if (beneficiaryRepository.existsByUserIdAndAccountNumber(user.userId(), request.accountNumber())) {
            throw new BankingException(HttpStatus.CONFLICT.value(), "Beneficiary already exists");
        }

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setUserId(user.userId());
        beneficiary.setNickname(request.nickname());
        beneficiary.setAccountNumber(request.accountNumber());
        beneficiary.setAccountHolderName(request.accountHolderName());

        return toResponse(beneficiaryRepository.save(beneficiary));
    }

    public List<BeneficiaryResponse> listBeneficiaries() {
        UUID userId = authContextService.getAuthenticatedUser().userId();
        return beneficiaryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(BeneficiaryService::toResponse)
                .toList();
    }

    @Transactional
    public void deleteBeneficiary(UUID beneficiaryId) {
        UUID userId = authContextService.getAuthenticatedUser().userId();
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(beneficiaryId, userId)
                .orElseThrow(() -> new BankingException(HttpStatus.NOT_FOUND.value(), "Beneficiary not found"));
        beneficiaryRepository.delete(beneficiary);
    }

    static BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getNickname(),
                beneficiary.getAccountNumber(),
                beneficiary.getAccountHolderName(),
                beneficiary.getCreatedAt()
        );
    }
}
