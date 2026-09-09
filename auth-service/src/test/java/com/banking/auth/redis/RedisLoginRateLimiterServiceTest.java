package com.banking.auth.redis;

import com.banking.auth.config.LoginRateLimitProperties;
import com.banking.common.exception.BankingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLoginRateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisLoginRateLimiterService service;

    @BeforeEach
    void setUp() {
        service = new RedisLoginRateLimiterService(
                redisTemplate,
                new LoginRateLimitProperties(5, 15)
        );
    }

    @Test
    void checkAllowedPassesWhenBelowLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:login:failures:alice@example.com")).thenReturn("3");

        assertThatCode(() -> service.checkAllowed("alice@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAllowedBlocksWhenLimitReached() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:login:failures:alice@example.com")).thenReturn("5");

        assertThatThrownBy(() -> service.checkAllowed("alice@example.com"))
                .isInstanceOf(BankingException.class)
                .extracting("status")
                .isEqualTo(429);
    }

    @Test
    void recordFailureIncrementsCounterAndSetsExpiryOnFirstAttempt() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("auth:login:failures:alice@example.com")).thenReturn(1L);

        service.recordFailure("alice@example.com");

        verify(redisTemplate).expire("auth:login:failures:alice@example.com", Duration.ofMinutes(15));
    }

    @Test
    void resetFailuresDeletesKey() {
        service.resetFailures("alice@example.com");

        verify(redisTemplate).delete("auth:login:failures:alice@example.com");
    }
}
