package com.finsight.transaction.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:txdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTransactionShouldReturnCreated() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .with(userJwt("alice@finsight.local"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreatePayload("Lunch")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.ownerEmail").value("alice@finsight.local"))
                .andExpect(jsonPath("$.category").value("Food"));
    }

    @Test
    void createTransactionShouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .with(userJwt("alice@finsight.local"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"occurredAt\": \"2026-03-28T10:15:30Z\",
                                  \"type\": \"EXPENSE\",
                                  \"category\": \"Food\",
                                  \"amount\": 0,
                                  \"currency\": \"INR\"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listShouldOnlyReturnCallerTransactions() throws Exception {
        createTransaction("alice@finsight.local", "Alice txn");
        createTransaction("bob@finsight.local", "Bob txn");

        mockMvc.perform(get("/api/transactions")
                        .with(userJwt("alice@finsight.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.description=='Alice txn')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.description=='Bob txn')]").isEmpty());
    }

    @Test
    void updateShouldRejectNonOwner() throws Exception {
        UUID id = createTransaction("owner@finsight.local", "Owner txn");

        mockMvc.perform(patch("/api/transactions/{id}", id)
                        .with(userJwt("other@finsight.local"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"category\": \"Bills\"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteShouldRemoveOwnedTransaction() throws Exception {
        UUID id = createTransaction("delete@finsight.local", "Delete txn");

        mockMvc.perform(delete("/api/transactions/{id}", id)
                        .with(userJwt("delete@finsight.local")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions/{id}", id)
                        .with(userJwt("delete@finsight.local")))
                .andExpect(status().isNotFound());
    }

    private UUID createTransaction(String email, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .with(userJwt(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreatePayload(description)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private String validCreatePayload(String description) {
        return """
                {
                  \"occurredAt\": \"2026-03-28T10:15:30Z\",
                  \"type\": \"EXPENSE\",
                  \"category\": \"Food\",
                  \"amount\": 250.75,
                  \"currency\": \"INR\",
                  \"description\": \"%s\",
                  \"merchant\": \"Cafe\"
                }
                """.formatted(description);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor userJwt(String email) {
        return jwt().jwt(token -> token.claim("sub", email).claim("role", "USER"));
    }
}



