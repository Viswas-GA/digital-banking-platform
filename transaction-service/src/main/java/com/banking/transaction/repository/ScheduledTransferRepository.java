package com.banking.transaction.repository;

import com.banking.transaction.domain.ScheduledTransfer;
import com.banking.transaction.domain.ScheduledTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, UUID> {

    List<ScheduledTransfer> findByUserIdOrderByScheduledAtDesc(UUID userId);

    Optional<ScheduledTransfer> findByIdAndUserId(UUID id, UUID userId);

    List<ScheduledTransfer> findByStatusAndScheduledAtLessThanEqual(
            ScheduledTransferStatus status,
            Instant scheduledAt
    );
}
