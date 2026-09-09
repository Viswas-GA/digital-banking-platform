package com.banking.auth.messaging;

import com.banking.auth.domain.User;

public record KycRejectedEvent(User user, User admin, String reason) {
}
