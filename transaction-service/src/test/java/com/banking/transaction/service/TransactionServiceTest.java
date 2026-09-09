package com.banking.transaction.service;

import com.banking.transaction.client.AccountServiceClient;
import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.dto.AccountTransferResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.security.AuthenticatedUser;
import com.banking.common.exception.BankingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private AuthContextService authContextService;

    @Mock
    private TransferLimitService transferLimitService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private TransactionService transactionService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void transferRecordsTransaction() {
        UUID userId = UUID.randomUUID();
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        when(authContextService.getAuthenticatedUser())
                .thenReturn(new AuthenticatedUser(userId, "user@example.com", "USER", "VERIFIED"));
        when(accountServiceClient.transfer(anyString(), any()))
                .thenReturn(new AccountTransferResponse(
                        fromId, "ACCT111", toId, "ACCT222",
                        new BigDecimal("100.00"), "INR", new BigDecimal("900.00"), "Rent"
                ));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        var response = transactionService.transfer(new TransferRequest(
                fromId, "ACCT222", new BigDecimal("100.00"), "Rent"));

        assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        assertThat(response.reference()).startsWith("TXN");
    }

    @Test
    void transferRequiresVerifiedKyc() {
        when(authContextService.getAuthenticatedUser())
                .thenReturn(new AuthenticatedUser(UUID.randomUUID(), "user@example.com", "USER", "PENDING"));

        assertThatThrownBy(() -> transactionService.transfer(new TransferRequest(
                UUID.randomUUID(), "ACCT222", new BigDecimal("10.00"), null)))
                .isInstanceOf(BankingException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN.value());
    }
}
