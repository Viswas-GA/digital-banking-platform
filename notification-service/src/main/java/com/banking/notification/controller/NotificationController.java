package com.banking.notification.controller;

import com.banking.notification.dto.NotificationResponse;
import com.banking.notification.dto.UnreadCountResponse;
import com.banking.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "In-app notifications for banking events")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    @SecurityRequirements
    public Map<String, String> health() {
        return Map.of(
                "service", "notification-service",
                "status", "UP"
        );
    }

    @GetMapping
    @Operation(summary = "List notifications", description = "Returns notifications for the authenticated user")
    public List<NotificationResponse> listMine() {
        return notificationService.listMine();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread count")
    public UnreadCountResponse unreadCount() {
        return notificationService.unreadCount();
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark as read")
    public NotificationResponse markAsRead(@PathVariable UUID id) {
        return notificationService.markAsRead(id);
    }
}
