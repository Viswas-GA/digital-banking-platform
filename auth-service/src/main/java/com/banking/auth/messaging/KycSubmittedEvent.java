package com.banking.auth.messaging;

import com.banking.auth.domain.User;

import java.util.UUID;

public record KycSubmittedEvent(User user, UUID submissionId) {
}
