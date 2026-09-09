package com.banking.auth.security;

import com.banking.auth.config.JwtProperties;
import com.banking.auth.domain.KycStatus;
import com.banking.auth.domain.User;
import com.banking.auth.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties("test-jwt-secret-key-for-banking-auth-service-tests-only", 3600000));
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setPasswordHash("hash");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setRole(UserRole.USER);
        user.setKycStatus(KycStatus.PENDING);
        user.setEnabled(true);
    }

    @Test
    void generatesAndValidatesToken() {
        String token = jwtService.generateToken(user);
        AuthUserDetails userDetails = new AuthUserDetails(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void rejectsTokenForDifferentUser() {
        String token = jwtService.generateToken(user);

        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setEmail("bob@example.com");
        otherUser.setPasswordHash("hash");
        otherUser.setRole(UserRole.USER);
        otherUser.setEnabled(true);

        assertThat(jwtService.isTokenValid(token, new AuthUserDetails(otherUser))).isFalse();
    }
}
