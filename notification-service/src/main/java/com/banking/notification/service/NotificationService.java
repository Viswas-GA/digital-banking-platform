package com.banking.notification.service;

import com.banking.notification.domain.Notification;
import com.banking.notification.dto.NotificationResponse;
import com.banking.notification.dto.UnreadCountResponse;
import com.banking.notification.repository.NotificationRepository;
import com.banking.notification.security.AuthenticatedUser;
import com.banking.common.exception.BankingException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationResponse> listMine() {
        UUID userId = getAuthenticatedUser().userId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationService::toResponse)
                .toList();
    }

    public UnreadCountResponse unreadCount() {
        UUID userId = getAuthenticatedUser().userId();
        return new UnreadCountResponse(notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Transactional
    public NotificationResponse markAsRead(UUID id) {
        UUID userId = getAuthenticatedUser().userId();
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BankingException(HttpStatus.NOT_FOUND.value(), "Notification not found"));

        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BankingException(HttpStatus.UNAUTHORIZED.value(), "Authentication required");
        }
        return user;
    }

    static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
