package com.banking.auth.controller;

import com.banking.auth.domain.User;
import com.banking.auth.domain.UserRole;
import com.banking.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class KycControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void userCanSubmitKycAndAdminCanApprove() throws Exception {
        String userEmail = "kyc-user-" + UUID.randomUUID() + "@example.com";
        String adminEmail = "kyc-admin-" + UUID.randomUUID() + "@example.com";

        String userToken = registerAndGetToken(userEmail);
        promoteToAdmin(adminEmail);

        String adminToken = loginAndGetToken(adminEmail);

        String submitPayload = """
                {
                  "documentType": "PASSPORT",
                  "documentNumber": "P1234567",
                  "dateOfBirth": "1995-05-15",
                  "addressLine1": "123 Main Street",
                  "addressLine2": "Apt 4",
                  "city": "Mumbai",
                  "state": "Maharashtra",
                  "postalCode": "400001",
                  "country": "India"
                }
                """;

        mockMvc.perform(post("/api/v1/kyc/submit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mockMvc.perform(get("/api/v1/kyc/status")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("SUBMITTED"));

        UUID userId = userRepository.findByEmailIgnoreCase(userEmail).orElseThrow().getId();

        mockMvc.perform(get("/api/v1/kyc/admin/pending")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == '" + userId + "')].documentNumber").value("P1234567"));

        mockMvc.perform(post("/api/v1/kyc/admin/{userId}/approve", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/v1/kyc/status")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("VERIFIED"));
    }

    @Test
    void regularUserCannotAccessAdminKycEndpoints() throws Exception {
        String userEmail = "kyc-regular-" + UUID.randomUUID() + "@example.com";
        String userToken = registerAndGetToken(userEmail);

        mockMvc.perform(get("/api/v1/kyc/admin/pending")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    private String registerAndGetToken(String email) throws Exception {
        String payload = """
                {
                  "email": "%s",
                  "password": "password123",
                  "firstName": "Kyc",
                  "lastName": "User"
                }
                """.formatted(email);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private String loginAndGetToken(String email) throws Exception {
        String payload = """
                {
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(email);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private void promoteToAdmin(String email) throws Exception {
        registerAndGetToken(email);
        User admin = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
    }
}
