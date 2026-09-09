package com.banking.auth.messaging;

import com.banking.auth.domain.User;

public record KycApprovedEvent(User user, User admin) {
}
