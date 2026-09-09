package com.banking.auth.service;

import com.banking.auth.domain.KycStatus;
import com.banking.auth.domain.User;
import com.banking.auth.domain.UserRole;
import com.banking.auth.dto.AuthResponse;
import com.banking.auth.dto.LoginRequest;
import com.banking.auth.dto.RegisterRequest;
import com.banking.auth.dto.UserResponse;
import com.banking.auth.messaging.UserRegisteredEvent;
import com.banking.auth.redis.LoginRateLimiterService;
import com.banking.auth.redis.TokenBlacklistService;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.security.JwtService;
import com.banking.common.exception.BankingException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuthContextService authContextService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final LoginRateLimiterService loginRateLimiterService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            AuthContextService authContextService,
            ApplicationEventPublisher applicationEventPublisher,
            LoginRateLimiterService loginRateLimiterService,
            TokenBlacklistService tokenBlacklistService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.authContextService = authContextService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.loginRateLimiterService = loginRateLimiterService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BankingException(HttpStatus.CONFLICT.value(), "Email is already registered");
        }

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setRole(UserRole.USER);
        user.setKycStatus(KycStatus.PENDING);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        applicationEventPublisher.publishEvent(new UserRegisteredEvent(savedUser));
        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        loginRateLimiterService.checkAllowed(request.email());

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> {
                    loginRateLimiterService.recordFailure(request.email());
                    return new BadCredentialsException("Invalid email or password");
                });

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getId().toString(),
                            request.password()
                    )
            );
        } catch (BadCredentialsException ex) {
            loginRateLimiterService.recordFailure(request.email());
            throw new BadCredentialsException("Invalid email or password");
        }

        loginRateLimiterService.resetFailures(request.email());
        return buildAuthResponse(user);
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BankingException(HttpStatus.UNAUTHORIZED.value(), "Authentication required");
        }

        String token = authorizationHeader.substring(7);
        long ttlMs = jwtService.getRemainingTtlMs(token);
        tokenBlacklistService.blacklist(token, ttlMs);
    }

    public UserResponse getCurrentUser() {
        User user = authContextService.getAuthenticatedUser();
        return toUserResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs() / 1000,
                toUserResponse(user)
        );
    }

    static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getKycStatus()
        );
    }
}
