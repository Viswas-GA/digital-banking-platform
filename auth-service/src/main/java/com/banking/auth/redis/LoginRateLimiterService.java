package com.banking.auth.redis;

public interface LoginRateLimiterService {

    void checkAllowed(String email);

    void recordFailure(String email);

    void resetFailures(String email);
}
