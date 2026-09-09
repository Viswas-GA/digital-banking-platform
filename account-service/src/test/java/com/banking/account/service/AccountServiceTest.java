package com.banking.account.service;

import com.banking.account.config.InternalProperties;
import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountType;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.dto.TransferRequest;
import com.banking.account.repository.AccountRepository;
import com.banking.account.security.AuthenticatedUser;
import com.banking.common.exception.BankingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private InternalProperties internalProperties;

    @InjectMocks
    private AccountService accountService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAccountRequiresVerifiedKyc() {
        setAuthenticatedUser("PENDING");

        assertThatThrownBy(() -> accountService.createAccount(new CreateAccountRequest(AccountType.SAVINGS, "INR")))
                .isInstanceOf(BankingException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void createAccountForVerifiedUser() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId, "VERIFIED");

        when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(UUID.randomUUID());
            return account;
        });

        var response = accountService.createAccount(new CreateAccountRequest(AccountType.SAVINGS, "INR"));

        assertThat(response.accountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.accountNumber()).startsWith("ACCT");
    }

    @Test
    void transferRejectsSameAccount() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId, "VERIFIED");

        Account account = activeAccount(userId, "ACCT1111111111", new BigDecimal("1000.00"));
        UUID accountId = UUID.randomUUID();
        account.setId(accountId);

        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(java.util.Optional.of(account));
        when(accountRepository.findByAccountNumber("ACCT1111111111")).thenReturn(java.util.Optional.of(account));

        assertThatThrownBy(() -> accountService.transfer(new TransferRequest(
                accountId, "ACCT1111111111", new BigDecimal("250.00"), "Self transfer")))
                .isInstanceOf(BankingException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.BAD_REQUEST.value(), "Cannot transfer to the same account");
    }

    @Test
    void transferMovesBalanceBetweenAccounts() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId, "VERIFIED");

        Account from = activeAccount(userId, "ACCT1111111111", new BigDecimal("1000.00"));
        UUID fromId = UUID.randomUUID();
        from.setId(fromId);
        Account to = activeAccount(UUID.randomUUID(), "ACCT2222222222", new BigDecimal("200.00"));

        when(accountRepository.findByIdAndUserId(fromId, userId)).thenReturn(java.util.Optional.of(from));
        when(accountRepository.findByAccountNumber("ACCT2222222222")).thenReturn(java.util.Optional.of(to));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = accountService.transfer(new TransferRequest(
                fromId, "ACCT2222222222", new BigDecimal("250.00"), "Payment"));

        assertThat(from.getBalance()).isEqualByComparingTo("750.00");
        assertThat(to.getBalance()).isEqualByComparingTo("450.00");
        assertThat(response.amount()).isEqualByComparingTo("250.00");
    }

    private Account activeAccount(UUID userId, String accountNumber, BigDecimal balance) {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUserId(userId);
        account.setAccountNumber(accountNumber);
        account.setCurrency("INR");
        account.setBalance(balance);
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }

    private void setAuthenticatedUser(String kycStatus) {
        setAuthenticatedUser(UUID.randomUUID(), kycStatus);
    }

    private void setAuthenticatedUser(UUID userId, String kycStatus) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "user@example.com", "USER", kycStatus);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null)
        );
    }
}
