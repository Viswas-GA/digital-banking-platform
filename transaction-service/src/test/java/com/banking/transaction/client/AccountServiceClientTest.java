package com.banking.transaction.client;

import com.banking.common.exception.BankingException;
import com.banking.transaction.config.AccountServiceProperties;
import com.banking.transaction.config.InternalProperties;
import com.banking.transaction.dto.AccountTransferRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AccountServiceClientTest {

    private MockRestServiceServer mockServer;
    private AccountServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        client = new AccountServiceClient(
                restClient,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                new AccountServiceProperties("http://localhost:8082"),
                new InternalProperties("test-secret")
        );
    }

    @AfterEach
    void verify() {
        mockServer.verify();
    }

    @Test
    void transferPropagatesAccountServiceValidationError() {
        UUID fromAccountId = UUID.randomUUID();
        String errorBody = """
                {
                  "timestamp": "%s",
                  "status": 400,
                  "error": "Bad Request",
                  "message": "Cannot transfer to the same account",
                  "path": "/api/v1/accounts/transfer"
                }
                """.formatted(Instant.now());

        mockServer.expect(requestTo("http://localhost:8082/api/v1/accounts/transfer"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body(errorBody).contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.transfer(
                "Bearer token",
                new AccountTransferRequest(fromAccountId, "ACCT1111111111", new BigDecimal("250.00"), "Rent")
        ))
                .isInstanceOf(BankingException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.BAD_REQUEST.value(), "Cannot transfer to the same account");
    }

    @Test
    void transferReturnsSuccessResponse() {
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        String successBody = """
                {
                  "fromAccountId": "%s",
                  "fromAccountNumber": "ACCT1111111111",
                  "toAccountId": "%s",
                  "toAccountNumber": "ACCT2222222222",
                  "amount": 250.00,
                  "currency": "INR",
                  "fromAccountBalance": 9750.00,
                  "description": "Rent"
                }
                """.formatted(fromAccountId, toAccountId);

        mockServer.expect(requestTo("http://localhost:8082/api/v1/accounts/transfer"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(successBody, MediaType.APPLICATION_JSON));

        var response = client.transfer(
                "Bearer token",
                new AccountTransferRequest(fromAccountId, "ACCT2222222222", new BigDecimal("250.00"), "Rent")
        );

        assertThat(response.amount()).isEqualByComparingTo("250.00");
        assertThat(response.fromAccountNumber()).isEqualTo("ACCT1111111111");
    }
}
