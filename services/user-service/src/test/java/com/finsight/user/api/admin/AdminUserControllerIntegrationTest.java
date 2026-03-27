package com.finsight.user.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void nonAdminCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(jwt().jwt(token -> token.claim("sub", "user@finsight.local").claim("role", "USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeactivateAndReactivateUser() throws Exception {
        String email = "bundle-user-" + UUID.randomUUID() + "@finsight.local";
        register(email, "Passw0rd!123");

        mockMvc.perform(patch("/api/admin/users/{email}/deactivate", email)
                        .with(adminJwt()))
                .andExpect(status().isNoContent());

        login(email, "Passw0rd!123", 401);

        mockMvc.perform(patch("/api/admin/users/{email}/activate", email)
                        .with(adminJwt()))
                .andExpect(status().isNoContent());

        login(email, "Passw0rd!123", 200);
    }

    @Test
    void nonAdminCannotDeactivateOrActivateUsers() throws Exception {
        String email = "forbidden-user-" + UUID.randomUUID() + "@finsight.local";
        register(email, "Passw0rd!123");

        mockMvc.perform(patch("/api/admin/users/{email}/deactivate", email)
                        .with(userJwt()))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/{email}/activate", email)
                        .with(userJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsersWithStatusFilters() throws Exception {
        String activeEmail = "active-list-" + UUID.randomUUID() + "@finsight.local";
        String inactiveEmail = "inactive-list-" + UUID.randomUUID() + "@finsight.local";
        register(activeEmail, "Passw0rd!123");
        register(inactiveEmail, "Passw0rd!123");

        mockMvc.perform(patch("/api/admin/users/{email}/deactivate", inactiveEmail)
                        .with(adminJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/users").param("status", "all")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.email=='%s')]".formatted(activeEmail)).isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.email=='%s')]".formatted(inactiveEmail)).isNotEmpty());

        mockMvc.perform(get("/api/admin/users").param("status", "active")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.email=='%s' && @.active==true)]".formatted(activeEmail)).isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.email=='%s')]".formatted(inactiveEmail)).isEmpty());

        mockMvc.perform(get("/api/admin/users").param("status", "inactive")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.email=='%s' && @.active==false)]".formatted(inactiveEmail)).isNotEmpty());
    }

    private void register(String email, String password) throws Exception {
        String payload = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private void login(String email, String password, int expectedStatus) throws Exception {
        String payload = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        var result = mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(expectedStatus))
                .andReturn();

        if (expectedStatus == 200) {
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            if (!"Bearer".equals(body.path("tokenType").asText())) {
                throw new AssertionError("Expected bearer token response");
            }
        }
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(token -> token.claim("sub", "admin@finsight.local").claim("role", "ADMIN"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
        return jwt().jwt(token -> token.claim("sub", "user@finsight.local").claim("role", "USER"));
    }
}

