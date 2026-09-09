package com.banking.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class GatewayController {

    @Value("${app.services.auth}")
    private String authService;

    @Value("${app.services.account}")
    private String accountService;

    @Value("${app.services.transaction}")
    private String transactionService;

    @Value("${app.services.notification}")
    private String notificationService;

    @Value("${app.services.audit}")
    private String auditService;

    @GetMapping("/api/v1/gateway/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "api-gateway");
        response.put("status", "UP");
        response.put("routes", Map.of(
                "auth", authService,
                "account", accountService,
                "transaction", transactionService,
                "notification", notificationService,
                "audit", auditService
        ));
        return response;
    }
}
