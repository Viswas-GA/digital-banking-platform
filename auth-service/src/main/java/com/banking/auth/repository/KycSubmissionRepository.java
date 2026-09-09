package com.banking.auth.repository;

import com.banking.auth.domain.KycSubmission;
import com.banking.auth.domain.KycSubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycSubmissionRepository extends JpaRepository<KycSubmission, UUID> {

    Optional<KycSubmission> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    List<KycSubmission> findByStatusOrderByCreatedAtAsc(KycSubmissionStatus status);

    boolean existsByUserIdAndStatus(UUID userId, KycSubmissionStatus status);
}
