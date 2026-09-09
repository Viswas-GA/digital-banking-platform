package com.banking.transaction.service;

import com.banking.transaction.client.AccountServiceClient;
import com.banking.transaction.domain.Transaction;
import com.banking.transaction.dto.AccountTransferRequest;
import com.banking.transaction.dto.AccountTransferResponse;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferLimitResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.messaging.TransferCompletedEvent;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.security.AuthenticatedUser;
import com.banking.common.exception.BankingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final AuthContextService authContextService;
    private final TransferLimitService transferLimitService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountServiceClient accountServiceClient,
            AuthContextService authContextService,
            TransferLimitService transferLimitService,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
        this.authContextService = authContextService;
        this.transferLimitService = transferLimitService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        AuthenticatedUser user = authContextService.getAuthenticatedUser();

        if (!user.isKycVerified()) {
            throw new BankingException(HttpStatus.FORBIDDEN.value(),
                    "KYC verification required for transfers. Please login again after KYC approval.");
        }

        transferLimitService.validateTransfer(user.userId(), request.amount());

        String authorizationHeader = getAuthorizationHeader();
        AccountTransferResponse transferResult = accountServiceClient.transfer(
                authorizationHeader,
                new AccountTransferRequest(
                        request.fromAccountId(),
                        request.toAccountNumber(),
                        request.amount(),
                        request.description()
                )
        );

        Transaction transaction = ScheduledTransferService.buildTransaction(user.userId(), transferResult);
        Transaction saved = transactionRepository.save(transaction);
        applicationEventPublisher.publishEvent(TransferCompletedEvent.immediate(saved, user.email(), user.role()));
        return toResponse(saved);
    }

    public TransferLimitResponse getTransferLimit() {
        UUID userId = authContextService.getAuthenticatedUser().userId();
        return transferLimitService.getLimitInfo(userId);
    }

    public List<TransactionResponse> getMyTransactions() {
        UUID userId = authContextService.getAuthenticatedUser().userId();
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(TransactionService::toResponse)
                .toList();
    }

    private String getAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BankingException(HttpStatus.UNAUTHORIZED.value(), "Authentication required");
        }
        HttpServletRequest request = attributes.getRequest();
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BankingException(HttpStatus.UNAUTHORIZED.value(), "Authentication required");
        }
        return header;
    }

    static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getReference(),
                transaction.getFromAccountId(),
                transaction.getFromAccountNumber(),
                transaction.getToAccountId(),
                transaction.getToAccountNumber(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
