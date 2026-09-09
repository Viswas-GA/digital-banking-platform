package com.banking.auth.service;

import com.banking.auth.domain.KycStatus;
import com.banking.auth.domain.KycSubmission;
import com.banking.auth.domain.KycSubmissionStatus;
import com.banking.auth.domain.User;
import com.banking.auth.dto.KycRejectRequest;
import com.banking.auth.dto.KycStatusResponse;
import com.banking.auth.dto.KycSubmissionResponse;
import com.banking.auth.dto.KycSubmitRequest;
import com.banking.auth.messaging.KycApprovedEvent;
import com.banking.auth.messaging.KycRejectedEvent;
import com.banking.auth.messaging.KycSubmittedEvent;
import com.banking.auth.repository.KycSubmissionRepository;
import com.banking.auth.repository.UserRepository;
import com.banking.common.exception.BankingException;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class KycService {

    private final KycSubmissionRepository kycSubmissionRepository;
    private final UserRepository userRepository;
    private final AuthContextService authContextService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public KycService(
            KycSubmissionRepository kycSubmissionRepository,
            UserRepository userRepository,
            AuthContextService authContextService,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.kycSubmissionRepository = kycSubmissionRepository;
        this.userRepository = userRepository;
        this.authContextService = authContextService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public KycSubmissionResponse submit(KycSubmitRequest request) {
        User user = authContextService.getAuthenticatedUser();

        if (user.getKycStatus() == KycStatus.VERIFIED) {
            throw new BankingException(HttpStatus.CONFLICT.value(), "KYC is already verified");
        }
        if (user.getKycStatus() == KycStatus.SUBMITTED) {
            throw new BankingException(HttpStatus.CONFLICT.value(), "KYC submission is already under review");
        }

        KycSubmission submission = new KycSubmission();
        submission.setUserId(user.getId());
        submission.setDocumentType(request.documentType());
        submission.setDocumentNumber(request.documentNumber());
        submission.setDateOfBirth(request.dateOfBirth());
        submission.setAddressLine1(request.addressLine1());
        submission.setAddressLine2(request.addressLine2());
        submission.setCity(request.city());
        submission.setState(request.state());
        submission.setPostalCode(request.postalCode());
        submission.setCountry(request.country());
        submission.setStatus(KycSubmissionStatus.SUBMITTED);

        user.setKycStatus(KycStatus.SUBMITTED);
        userRepository.save(user);

        KycSubmission saved = kycSubmissionRepository.save(submission);
        applicationEventPublisher.publishEvent(new KycSubmittedEvent(user, saved.getId()));
        return toResponse(saved);
    }

    public KycStatusResponse getStatus() {
        User user = authContextService.getAuthenticatedUser();
        return buildStatusResponse(user);
    }

    public List<KycSubmissionResponse> listPendingSubmissions() {
        return kycSubmissionRepository.findByStatusOrderByCreatedAtAsc(KycSubmissionStatus.SUBMITTED)
                .stream()
                .map(KycService::toResponse)
                .toList();
    }

    @Transactional
    public KycSubmissionResponse approve(UUID userId) {
        User user = getUserForReview(userId);
        KycSubmission submission = getLatestSubmitted(userId);

        submission.setStatus(KycSubmissionStatus.APPROVED);
        submission.setReviewedAt(Instant.now());
        submission.setReviewedBy(authContextService.getAuthenticatedUserId());
        submission.setRejectionReason(null);

        user.setKycStatus(KycStatus.VERIFIED);
        userRepository.save(user);

        KycSubmissionResponse response = toResponse(kycSubmissionRepository.save(submission));
        applicationEventPublisher.publishEvent(new KycApprovedEvent(user, authContextService.getAuthenticatedUser()));
        return response;
    }

    @Transactional
    public KycSubmissionResponse reject(UUID userId, KycRejectRequest request) {
        User user = getUserForReview(userId);
        KycSubmission submission = getLatestSubmitted(userId);

        submission.setStatus(KycSubmissionStatus.REJECTED);
        submission.setReviewedAt(Instant.now());
        submission.setReviewedBy(authContextService.getAuthenticatedUserId());
        submission.setRejectionReason(request.reason());

        user.setKycStatus(KycStatus.REJECTED);
        userRepository.save(user);

        KycSubmissionResponse response = toResponse(kycSubmissionRepository.save(submission));
        applicationEventPublisher.publishEvent(new KycRejectedEvent(user, authContextService.getAuthenticatedUser(), request.reason()));
        return response;
    }

    private User getUserForReview(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BankingException(HttpStatus.NOT_FOUND.value(), "User not found"));
    }

    private KycSubmission getLatestSubmitted(UUID userId) {
        KycSubmission submission = kycSubmissionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BankingException(HttpStatus.NOT_FOUND.value(), "No KYC submission found"));

        if (submission.getStatus() != KycSubmissionStatus.SUBMITTED) {
            throw new BankingException(HttpStatus.CONFLICT.value(), "No pending KYC submission for this user");
        }
        return submission;
    }

    private KycStatusResponse buildStatusResponse(User user) {
        KycSubmissionResponse latest = kycSubmissionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .map(KycService::toResponse)
                .orElse(null);
        return new KycStatusResponse(user.getKycStatus(), latest);
    }

    static KycSubmissionResponse toResponse(KycSubmission submission) {
        return new KycSubmissionResponse(
                submission.getId(),
                submission.getUserId(),
                submission.getDocumentType(),
                submission.getDocumentNumber(),
                submission.getDateOfBirth(),
                submission.getAddressLine1(),
                submission.getAddressLine2(),
                submission.getCity(),
                submission.getState(),
                submission.getPostalCode(),
                submission.getCountry(),
                submission.getStatus(),
                submission.getRejectionReason(),
                submission.getReviewedAt(),
                submission.getReviewedBy(),
                submission.getCreatedAt()
        );
    }
}
