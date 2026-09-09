package com.banking.transaction.service;

import com.banking.transaction.config.TransferProperties;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.dto.TransferLimitResponse;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.common.exception.BankingException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class TransferLimitService {

    private final TransactionRepository transactionRepository;
    private final TransferProperties transferProperties;

    public TransferLimitService(
            TransactionRepository transactionRepository,
            TransferProperties transferProperties
    ) {
        this.transactionRepository = transactionRepository;
        this.transferProperties = transferProperties;
    }

    public TransferLimitResponse getLimitInfo(UUID userId) {
        BigDecimal usedToday = getUsedToday(userId);
        BigDecimal dailyLimit = transferProperties.dailyLimit();
        BigDecimal remaining = dailyLimit.subtract(usedToday).max(BigDecimal.ZERO);
        return new TransferLimitResponse(dailyLimit, usedToday, remaining);
    }

    public void validateTransfer(UUID userId, BigDecimal amount) {
        BigDecimal usedToday = getUsedToday(userId);
        BigDecimal dailyLimit = transferProperties.dailyLimit();
        if (usedToday.add(amount).compareTo(dailyLimit) > 0) {
            BigDecimal remaining = dailyLimit.subtract(usedToday).max(BigDecimal.ZERO);
            throw new BankingException(HttpStatus.BAD_REQUEST.value(),
                    "Daily transfer limit exceeded. Limit: " + dailyLimit
                            + ", used today: " + usedToday
                            + ", remaining: " + remaining);
        }
    }

    private BigDecimal getUsedToday(UUID userId) {
        Instant startOfDay = LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
        return transactionRepository.sumAmountByUserSince(
                userId,
                TransactionStatus.COMPLETED,
                startOfDay
        );
    }
}
