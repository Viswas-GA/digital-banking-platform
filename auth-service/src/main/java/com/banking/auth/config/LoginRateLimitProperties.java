package com.banking.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit.login")
public record LoginRateLimitProperties(int maxAttempts, long windowMinutes) {
}
