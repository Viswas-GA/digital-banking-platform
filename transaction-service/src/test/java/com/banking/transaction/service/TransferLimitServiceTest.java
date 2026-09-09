package com.banking.transaction.service;

import com.banking.transaction.config.TransferProperties;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.common.exception.BankingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferLimitServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferLimitService transferLimitService;

    @Test
    void validateTransferAllowsWithinLimit() {
        UUID userId = UUID.randomUUID();
        when(transactionRepository.sumAmountByUserSince(
                eq(userId), eq(TransactionStatus.COMPLETED), org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(new BigDecimal("10000.00"));

        TransferProperties properties = new TransferProperties(new BigDecimal("50000"));
        transferLimitService = new TransferLimitService(transactionRepository, properties);

        transferLimitService.validateTransfer(userId, new BigDecimal("1000.00"));
    }

    @Test
    void validateTransferRejectsOverLimit() {
        UUID userId = UUID.randomUUID();
        when(transactionRepository.sumAmountByUserSince(
                eq(userId), eq(TransactionStatus.COMPLETED), org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(new BigDecimal("49000.00"));

        TransferProperties properties = new TransferProperties(new BigDecimal("50000"));
        transferLimitService = new TransferLimitService(transactionRepository, properties);

        assertThatThrownBy(() -> transferLimitService.validateTransfer(userId, new BigDecimal("2000.00")))
                .isInstanceOf(BankingException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void getLimitInfoReturnsRemaining() {
        UUID userId = UUID.randomUUID();
        when(transactionRepository.sumAmountByUserSince(
                eq(userId), eq(TransactionStatus.COMPLETED), org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(new BigDecimal("5000.00"));

        TransferProperties properties = new TransferProperties(new BigDecimal("50000"));
        transferLimitService = new TransferLimitService(transactionRepository, properties);

        var info = transferLimitService.getLimitInfo(userId);
        assertThat(info.dailyLimit()).isEqualByComparingTo("50000");
        assertThat(info.usedToday()).isEqualByComparingTo("5000");
        assertThat(info.remaining()).isEqualByComparingTo("45000");
    }
}
