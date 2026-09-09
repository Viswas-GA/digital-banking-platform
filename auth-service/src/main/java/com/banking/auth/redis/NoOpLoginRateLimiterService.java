package com.banking.auth.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false")
public class NoOpLoginRateLimiterService implements LoginRateLimiterService {

    @Override
    public void checkAllowed(String email) {
        // Redis disabled — no rate limiting
    }

    @Override
    public void recordFailure(String email) {
        // Redis disabled — no rate limiting
    }

    @Override
    public void resetFailures(String email) {
        // Redis disabled — no rate limiting
    }
}
