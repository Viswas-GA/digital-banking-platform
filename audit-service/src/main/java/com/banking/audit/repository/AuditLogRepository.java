package com.banking.audit.repository;

import com.banking.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    boolean existsByEventId(UUID eventId);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(
            @Param("action") String action,
            @Param("actorUserId") UUID actorUserId,
            Pageable pageable
    );
}
