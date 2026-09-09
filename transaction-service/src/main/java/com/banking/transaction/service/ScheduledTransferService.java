package com.banking.transaction.service;

import com.banking.transaction.client.AccountServiceClient;
import com.banking.transaction.domain.ScheduledTransfer;
import com.banking.transaction.domain.ScheduledTransferStatus;
import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.dto.AccountTransferResponse;
import com.banking.transaction.dto.InternalTransferRequest;
import com.banking.transaction.dto.ScheduleTransferRequest;
import com.banking.transaction.dto.ScheduledTransferResponse;
import com.banking.transaction.messaging.ScheduledTransferFailedEvent;
import com.banking.transaction.messaging.TransferCompletedEvent;
import com.banking.transaction.repository.ScheduledTransferRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.security.AuthenticatedUser;
import com.banking.common.exception.BankingException;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ScheduledTransferService {

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final AuthContextService authContextService;
    private final TransferLimitService transferLimitService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ScheduledTransferService(
            ScheduledTransferRepository scheduledTransferRepository,
            TransactionRepository transactionRepository,
            AccountServiceClient accountServiceClient,
            AuthContextService authContextService,
            TransferLimitService transferLimitService,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.scheduledTransferRepository = scheduledTransferRepository;
        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
        this.authContextService = authContextService;
        this.transferLimitService = transferLimitService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public ScheduledTransferResponse schedule(ScheduleTransferRequest request) {
        AuthenticatedUser user = authContextService.getAuthenticatedUser();
        requireVerifiedKyc(user);
        transferLimitService.validateTransfer(user.userId(), request.amount());

        ScheduledTransfer scheduled = new ScheduledTransfer();
        scheduled.setUserId(user.userId());
        scheduled.setFromAccountId(request.fromAccountId());
        scheduled.setToAccountNumber(request.toAccountNumber());
        scheduled.setAmount(request.amount());
        scheduled.setDescription(request.description());
        scheduled.setScheduledAt(request.scheduledAt());
        scheduled.setStatus(ScheduledTransferStatus.PENDING);

        return toResponse(scheduledTransferRepository.save(scheduled));
    }

    public List<ScheduledTransferResponse> listMine() {
        UUID userId = authContextService.getAuthenticatedUser().userId();
        return scheduledTransferRepository.findByUserIdOrderByScheduledAtDesc(userId)
                .stream()
                .map(ScheduledTransferService::toResponse)
                .toList();
    }

    @Transactional
    public ScheduledTransferResponse cancel(UUID id) {
        UUID userId = authContextService.getAuthenticatedUser().userId();
        ScheduledTransfer scheduled = scheduledTransferRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BankingException(HttpStatus.NOT_FOUND.value(), "Scheduled transfer not found"));

        if (scheduled.getStatus() != ScheduledTransferStatus.PENDING) {
            throw new BankingException(HttpStatus.BAD_REQUEST.value(),
                    "Only pending scheduled transfers can be cancelled");
        }

        scheduled.setStatus(ScheduledTransferStatus.CANCELLED);
        return toResponse(scheduledTransferRepository.save(scheduled));
    }

    @Transactional
    public void processDueTransfers() {
        List<ScheduledTransfer> dueTransfers = scheduledTransferRepository.findByStatusAndScheduledAtLessThanEqual(
                ScheduledTransferStatus.PENDING,
                Instant.now()
        );

        for (ScheduledTransfer scheduled : dueTransfers) {
            processOne(scheduled);
        }
    }

    private void processOne(ScheduledTransfer scheduled) {
        try {
            transferLimitService.validateTransfer(scheduled.getUserId(), scheduled.getAmount());

            AccountTransferResponse result = accountServiceClient.internalTransfer(
                    new InternalTransferRequest(
                            scheduled.getUserId(),
                            scheduled.getFromAccountId(),
                            scheduled.getToAccountNumber(),
                            scheduled.getAmount(),
                            scheduled.getDescription()
                    )
            );

            Transaction transaction = buildTransaction(scheduled.getUserId(), result);
            transaction = transactionRepository.save(transaction);

            scheduled.setStatus(ScheduledTransferStatus.COMPLETED);
            scheduled.setTransactionId(transaction.getId());
            scheduled.setFailureReason(null);
            scheduledTransferRepository.save(scheduled);
            applicationEventPublisher.publishEvent(TransferCompletedEvent.scheduled(transaction));
        } catch (Exception ex) {
            scheduled.setStatus(ScheduledTransferStatus.FAILED);
            scheduled.setFailureReason(ex.getMessage());
            scheduledTransferRepository.save(scheduled);
            applicationEventPublisher.publishEvent(new ScheduledTransferFailedEvent(
                    scheduled.getUserId(),
                    scheduled.getId(),
                    scheduled.getAmount(),
                    scheduled.getToAccountNumber(),
                    ex.getMessage()
            ));
        }
    }

    static Transaction buildTransaction(UUID userId, AccountTransferResponse transferResult) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setFromAccountId(transferResult.fromAccountId());
        transaction.setToAccountId(transferResult.toAccountId());
        transaction.setFromAccountNumber(transferResult.fromAccountNumber());
        transaction.setToAccountNumber(transferResult.toAccountNumber());
        transaction.setAmount(transferResult.amount());
        transaction.setCurrency(transferResult.currency());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription(transferResult.description());
        transaction.setReference(generateReference());
        return transaction;
    }

    private static void requireVerifiedKyc(AuthenticatedUser user) {
        if (!user.isKycVerified()) {
            throw new BankingException(HttpStatus.FORBIDDEN.value(),
                    "KYC verification required for transfers. Please login again after KYC approval.");
        }
    }

    private static String generateReference() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    static ScheduledTransferResponse toResponse(ScheduledTransfer scheduled) {
        return new ScheduledTransferResponse(
                scheduled.getId(),
                scheduled.getFromAccountId(),
                scheduled.getToAccountNumber(),
                scheduled.getAmount(),
                scheduled.getDescription(),
                scheduled.getScheduledAt(),
                scheduled.getStatus(),
                scheduled.getFailureReason(),
                scheduled.getTransactionId(),
                scheduled.getCreatedAt()
        );
    }
}
