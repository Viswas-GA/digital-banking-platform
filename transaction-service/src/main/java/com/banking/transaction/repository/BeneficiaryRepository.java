package com.banking.transaction.repository;

import com.banking.transaction.domain.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    List<Beneficiary> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Beneficiary> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndAccountNumber(UUID userId, String accountNumber);
}
