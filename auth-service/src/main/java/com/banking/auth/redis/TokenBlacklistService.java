package com.banking.auth.redis;

public interface TokenBlacklistService {

    void blacklist(String token, long ttlMs);

    boolean isBlacklisted(String token);
}
