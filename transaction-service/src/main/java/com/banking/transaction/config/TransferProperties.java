package com.banking.transaction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.transfer")
public record TransferProperties(BigDecimal dailyLimit) {
}
