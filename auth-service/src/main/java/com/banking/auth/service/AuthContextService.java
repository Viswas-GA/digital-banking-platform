package com.banking.auth.service;

import com.banking.auth.domain.User;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.security.AuthUserDetails;
import com.banking.common.exception.BankingException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthContextService {

    private final UserRepository userRepository;

    public AuthContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthUserDetails getAuthenticatedUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserDetails principal)) {
            throw new BankingException(HttpStatus.UNAUTHORIZED.value(), "Authentication required");
        }
        return principal;
    }

    public User getAuthenticatedUser() {
        AuthUserDetails principal = getAuthenticatedUserDetails();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new BankingException(HttpStatus.NOT_FOUND.value(), "User not found"));
    }

    public UUID getAuthenticatedUserId() {
        return getAuthenticatedUserDetails().getId();
    }
}
