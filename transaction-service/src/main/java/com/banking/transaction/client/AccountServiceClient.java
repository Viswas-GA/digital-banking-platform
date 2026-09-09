package com.banking.transaction.client;

import com.banking.common.exception.BankingException;
import com.banking.common.exception.ErrorResponse;
import com.banking.transaction.config.AccountServiceProperties;
import com.banking.transaction.config.InternalProperties;
import com.banking.transaction.dto.AccountTransferRequest;
import com.banking.transaction.dto.AccountTransferResponse;
import com.banking.transaction.dto.InternalTransferRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AccountServiceClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AccountServiceProperties accountServiceProperties;
    private final InternalProperties internalProperties;

    public AccountServiceClient(
            RestClient restClient,
            ObjectMapper objectMapper,
            AccountServiceProperties accountServiceProperties,
            InternalProperties internalProperties
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.accountServiceProperties = accountServiceProperties;
        this.internalProperties = internalProperties;
    }

    public AccountTransferResponse transfer(String authorizationHeader, AccountTransferRequest request) {
        try {
            return restClient.post()
                    .uri(accountServiceProperties.baseUrl() + "/api/v1/accounts/transfer")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .body(request)
                    .retrieve()
                    .body(AccountTransferResponse.class);
        } catch (RestClientResponseException ex) {
            throw mapAccountServiceError(ex);
        } catch (RestClientException ex) {
            throw new BankingException(HttpStatus.BAD_GATEWAY.value(),
                    "Account service unavailable. Make sure account-service is running on port 8082.");
        }
    }

    public AccountTransferResponse internalTransfer(InternalTransferRequest request) {
        try {
            return restClient.post()
                    .uri(accountServiceProperties.baseUrl() + "/api/v1/accounts/internal/transfer")
                    .header("X-Internal-Secret", internalProperties.secret())
                    .body(request)
                    .retrieve()
                    .body(AccountTransferResponse.class);
        } catch (RestClientResponseException ex) {
            throw mapAccountServiceError(ex);
        } catch (RestClientException ex) {
            throw new BankingException(HttpStatus.BAD_GATEWAY.value(),
                    "Account service unavailable for scheduled transfer.");
        }
    }

    private BankingException mapAccountServiceError(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            try {
                ErrorResponse error = objectMapper.readValue(body, ErrorResponse.class);
                int status = error.status() > 0 ? error.status() : ex.getStatusCode().value();
                String message = error.message() != null && !error.message().isBlank()
                        ? error.message()
                        : ex.getStatusText();
                return new BankingException(status, message);
            } catch (Exception ignored) {
                // Fall through to status text below.
            }
        }
        return new BankingException(ex.getStatusCode().value(), ex.getStatusText());
    }
}
