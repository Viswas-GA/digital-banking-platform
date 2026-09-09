package com.banking.transaction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.account-service")
public record AccountServiceProperties(String baseUrl) {
}
