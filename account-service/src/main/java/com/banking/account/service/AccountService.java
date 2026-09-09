package com.banking.account.service;

import com.banking.account.config.InternalProperties;
import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.BalanceResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.dto.InternalTransferRequest;
import com.banking.account.dto.TransferRequest;
import com.banking.account.dto.TransferResponse;
import com.banking.account.repository.AccountRepository;
import com.banking.account.security.AuthenticatedUser;
import com.banking.common.exception.BankingException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final InternalProperties internalProperties;

    public AccountService(AccountRepository accountRepository, InternalProperties internalProperties) {
        this.accountRepository = accountRepository;
        this.internalProperties = internalProperties;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();

        if (!user.isKycVerified()) {
            throw new BankingException(HttpStatus.FORBIDDEN.value(),
                    "KYC verification required before opening an account. Please complete KYC and login again.");
        }

        Account account = new Account();
        account.setUserId(user.userId());
        account.setAccountType(request.accountType());
        account.setCurrency(request.currency() != null ? request.currency() : "INR");
        account.setAccountNumber(generateAccountNumber());
        account.setStatus(AccountStatus.ACTIVE);

        return toResponse(accountRepository.save(account));
    }

    public List<AccountResponse> listMyAccounts() {
        UUID userId = getAuthenticatedUser().userId();
        return accountRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AccountService::toResponse)
                .toList();
    }

    public AccountResponse getAccount(UUID accountId) {
        Account account = getOwnedAccount(accountId);
        return toResponse(account);
    }

    public BalanceResponse getBalance(UUID accountId) {
        Account account = getOwnedAccount(accountId);
        return new BalanceResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCurrency(),
                account.getBalance()
        );
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();

        if (!user.isKycVerified()) {
            throw new BankingException(HttpStatus.FORBIDDEN.value(), "KYC verification required for transfers");
        }

        return executeTransfer(user.userId(), request);
    }

    @Transactional
    public TransferResponse internalTransfer(String secret, InternalTransferRequest request) {
        if (secret == null || !secret.equals(internalProperties.secret())) {
            throw new BankingException(HttpStatus.UNAUTHORIZED.value(), "Invalid internal secret");
        }

        TransferRequest transferRequest = new TransferRequest(
                request.fromAccountId(),
                request.toAccountNumber(),
                request.amount(),
                request.description()
        );
        return executeTransfer(request.userId(), transferRequest);
    }

    private TransferResponse executeTransfer(UUID userId, TransferRequest request) {
        Account fromAccount = accountRepository.findByIdAndUserId(request.fromAccountId(), userId)
                .orElseThrow(() -> new BankingException(HttpStatus.NOT_FOUND.value(), "Source account not found"));

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BankingException(HttpStatus.BAD_REQUEST.value(), "Source account is not active");
        }

        Account toAccount = accountRepository.findByAccountNumber(request.toAccountNumber())
                .orElseThrow(() -> new BankingException(HttpStatus.NOT_FOUND.value(), "Destination account not found"));

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BankingException(HttpStatus.BAD_REQUEST.value(), "Destination account is not active");
        }

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new BankingException(HttpStatus.BAD_REQUEST.value(), "Cannot transfer to the same account");
        }

        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new BankingException(HttpStatus.BAD_REQUEST.value(), "Currency mismatch between accounts");
        }

        if (fromAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new BankingException(HttpStatus.BAD_REQUEST.value(), "Insufficient balance");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.amount()));
        toAccount.setBalance(toAccount.getBalance().add(request.amount()));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        return new TransferResponse(
                fromAccount.getId(),
                fromAccount.getAccountNumber(),
                toAccount.getId(),
                toAccount.getAccountNumber(),
                request.amount(),
                fromAccount.getCurrency(),
                fromAccount.getBalance(),
                request.description()
        );
    }

    private Account getOwnedAccount(UUID accountId) {
        UUID userId = getAuthenticatedUser().userId();
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BankingException(HttpStatus.NOT_FOUND.value(), "Account not found"));
    }

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BankingException(HttpStatus.UNAUTHORIZED.value(), "Authentication required");
        }
        return user;
    }

    private String generateAccountNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "ACCT" + String.format("%010d", RANDOM.nextInt(1_000_000_000));
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new BankingException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Could not generate account number");
    }

    static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
