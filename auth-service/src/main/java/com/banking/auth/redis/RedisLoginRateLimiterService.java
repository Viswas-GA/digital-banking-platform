package com.banking.auth.redis;

import com.banking.auth.config.LoginRateLimitProperties;
import com.banking.common.exception.BankingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisLoginRateLimiterService implements LoginRateLimiterService {

    private static final String KEY_PREFIX = "auth:login:failures:";

    private final StringRedisTemplate redisTemplate;
    private final LoginRateLimitProperties properties;

    public RedisLoginRateLimiterService(
            StringRedisTemplate redisTemplate,
            LoginRateLimitProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void checkAllowed(String email) {
        String key = keyFor(email);
        String attempts = redisTemplate.opsForValue().get(key);
        if (attempts != null && Integer.parseInt(attempts) >= properties.maxAttempts()) {
            throw new BankingException(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too many failed login attempts. Please try again later."
            );
        }
    }

    @Override
    public void recordFailure(String email) {
        String key = keyFor(email);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(properties.windowMinutes()));
        }
    }

    @Override
    public void resetFailures(String email) {
        redisTemplate.delete(keyFor(email));
    }

    private String keyFor(String email) {
        return KEY_PREFIX + email.trim().toLowerCase();
    }
}
