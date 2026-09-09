package com.banking.auth.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisTokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new RedisTokenBlacklistService(redisTemplate);
    }

    @Test
    void blacklistStoresTokenHashWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.blacklist("jwt-token", 60_000L);

        verify(valueOperations).set(anyString(), eq("1"), eq(Duration.ofMillis(60_000L)));
    }

    @Test
    void blacklistSkipsZeroTtl() {
        service.blacklist("jwt-token", 0L);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void isBlacklistedReturnsTrueWhenKeyExists() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        assertThat(service.isBlacklisted("jwt-token")).isTrue();
    }
}
