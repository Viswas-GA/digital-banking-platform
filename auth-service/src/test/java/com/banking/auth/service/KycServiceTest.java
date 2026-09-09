package com.banking.auth.service;

import com.banking.auth.domain.DocumentType;
import com.banking.auth.domain.KycStatus;
import com.banking.auth.domain.KycSubmission;
import com.banking.auth.domain.KycSubmissionStatus;
import com.banking.auth.domain.User;
import com.banking.auth.domain.UserRole;
import com.banking.auth.dto.KycRejectRequest;
import com.banking.auth.dto.KycSubmitRequest;
import com.banking.auth.repository.KycSubmissionRepository;
import com.banking.auth.repository.UserRepository;
import com.banking.common.exception.BankingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock
    private KycSubmissionRepository kycSubmissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthContextService authContextService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private KycService kycService;

    @Test
    void submitCreatesSubmissionAndUpdatesUserStatus() {
        UUID userId = UUID.randomUUID();
        User user = pendingUser(userId);

        when(authContextService.getAuthenticatedUser()).thenReturn(user);
        when(kycSubmissionRepository.save(any(KycSubmission.class))).thenAnswer(invocation -> {
            KycSubmission submission = invocation.getArgument(0);
            submission.setId(UUID.randomUUID());
            return submission;
        });

        var response = kycService.submit(sampleSubmitRequest());

        assertThat(response.status()).isEqualTo(KycSubmissionStatus.SUBMITTED);
        assertThat(user.getKycStatus()).isEqualTo(KycStatus.SUBMITTED);
        verify(userRepository).save(user);
    }

    @Test
    void submitRejectsVerifiedUser() {
        User user = pendingUser(UUID.randomUUID());
        user.setKycStatus(KycStatus.VERIFIED);
        when(authContextService.getAuthenticatedUser()).thenReturn(user);

        assertThatThrownBy(() -> kycService.submit(sampleSubmitRequest()))
                .isInstanceOf(BankingException.class)
                .hasMessageContaining("already verified");
    }

    @Test
    void approveMarksUserVerified() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        User user = pendingUser(userId);
        user.setKycStatus(KycStatus.SUBMITTED);

        KycSubmission submission = new KycSubmission();
        submission.setId(UUID.randomUUID());
        submission.setUserId(userId);
        submission.setStatus(KycSubmissionStatus.SUBMITTED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(kycSubmissionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(submission));
        when(authContextService.getAuthenticatedUserId()).thenReturn(adminId);
        when(kycSubmissionRepository.save(submission)).thenReturn(submission);

        var response = kycService.approve(userId);

        assertThat(response.status()).isEqualTo(KycSubmissionStatus.APPROVED);
        assertThat(user.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
    }

    @Test
    void rejectMarksUserRejectedWithReason() {
        UUID userId = UUID.randomUUID();
        User user = pendingUser(userId);
        user.setKycStatus(KycStatus.SUBMITTED);

        KycSubmission submission = new KycSubmission();
        submission.setId(UUID.randomUUID());
        submission.setUserId(userId);
        submission.setStatus(KycSubmissionStatus.SUBMITTED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(kycSubmissionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(submission));
        when(authContextService.getAuthenticatedUserId()).thenReturn(UUID.randomUUID());
        when(kycSubmissionRepository.save(submission)).thenReturn(submission);

        var response = kycService.reject(userId, new KycRejectRequest("Document image is unclear"));

        assertThat(response.status()).isEqualTo(KycSubmissionStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Document image is unclear");
        assertThat(user.getKycStatus()).isEqualTo(KycStatus.REJECTED);
    }

    private static User pendingUser(UUID userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setRole(UserRole.USER);
        user.setKycStatus(KycStatus.PENDING);
        user.setEnabled(true);
        return user;
    }

    private static KycSubmitRequest sampleSubmitRequest() {
        return new KycSubmitRequest(
                DocumentType.PASSPORT,
                "P1234567",
                LocalDate.of(1995, 5, 15),
                "123 Main Street",
                "Apt 4",
                "Mumbai",
                "Maharashtra",
                "400001",
                "India"
        );
    }
}
