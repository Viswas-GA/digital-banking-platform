package com.banking.auth.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false")
public class NoOpTokenBlacklistService implements TokenBlacklistService {

    @Override
    public void blacklist(String token, long ttlMs) {
        // Redis disabled — logout is a no-op for token invalidation
    }

    @Override
    public boolean isBlacklisted(String token) {
        return false;
    }
}
