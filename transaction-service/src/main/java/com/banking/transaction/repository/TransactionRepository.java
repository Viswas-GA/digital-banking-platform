package com.banking.transaction.repository;

import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.userId = :userId
              AND t.status = :status
              AND t.createdAt >= :startOfDay
            """)
    BigDecimal sumAmountByUserSince(
            @Param("userId") UUID userId,
            @Param("status") TransactionStatus status,
            @Param("startOfDay") Instant startOfDay
    );
}
