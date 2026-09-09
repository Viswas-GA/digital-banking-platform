package com.banking.transaction.controller;

import com.banking.transaction.support.JwtTestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BeneficiaryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Test
    void addListAndDeleteBeneficiary() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = JwtTestHelper.createToken(userId, "user@example.com", "USER", "VERIFIED", jwtSecret);

        mockMvc.perform(post("/api/v1/beneficiaries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "Alice",
                                  "accountNumber": "ACCT9999999999",
                                  "accountHolderName": "Alice Smith"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("Alice"));

        mockMvc.perform(get("/api/v1/beneficiaries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").value("ACCT9999999999"));
    }
}
