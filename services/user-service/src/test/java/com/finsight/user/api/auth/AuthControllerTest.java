package com.finsight.user.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:userdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String PUBLIC_KEY_PEM = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArW7FXQCJeeUm/la84gPQ
            vZV91fsWbhe/URGkzjKDTV/t0H1hbYeD/tJlqJMnRcmHhi6gbJiPULj4siffH0+u
            tZd3XOS9xOa8Ujuxc1DPLWAt3wgmI/IYqPmuhalikNNsAwUR2T/9DHnFmpl/PXlT
            9aFVDsWyG0iN8Kb3F825r0wI5UGiad+OM50rCT7IjD9RTquCtM5LaeX6I2aJzIN+
            Ol92y/UmMAZFmknzeJ+37rLfQtURz+CF1HsOlQMcqAGuvHWdNpabwF/B39XlK59l
            WOAXlPyozGH10AIzWHvhiKKo9qssXDftthpjqZKD2DQNQUUDcaeYOMD6aacfdIcn
            PQIDAQAB
            -----END PUBLIC KEY-----
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerShouldCreateUserWithStrongPassword() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@finsight.local";

        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "StrongP@ss1"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void registerShouldRejectDuplicateEmail() throws Exception {
        String email = "duplicate-" + UUID.randomUUID() + "@finsight.local";
        String payload = """
                {
                  "email": "%s",
                  "password": "StrongP@ss1"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void registerShouldRejectWeakPassword() throws Exception {
        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "weak-password@finsight.local",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginShouldReturnTokenPairForValidCredentials() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "demo@finsight.local",
                                  "password": "Passw0rd!123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = response.get("accessToken").asText();

        Claims claims = Jwts.parser()
                .verifyWith(parsePublicKey(PUBLIC_KEY_PEM))
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();

        org.junit.jupiter.api.Assertions.assertEquals("demo@finsight.local", claims.getSubject());
        org.junit.jupiter.api.Assertions.assertEquals("finsight-user-service", claims.getIssuer());
        org.junit.jupiter.api.Assertions.assertEquals("USER", claims.get("role", String.class));
        org.junit.jupiter.api.Assertions.assertEquals("access", claims.get("typ", String.class));
    }

    @Test
    void refreshShouldRotateTokenAndRejectReuse() throws Exception {
        String email = "refresh-" + UUID.randomUUID() + "@finsight.local";
        String password = "StrongP@ss1";
        registerUser(email, password);

        JsonNode login = login(email, password);
        String refreshToken = login.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/users/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "%s" }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn();

        JsonNode refreshed = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertNotEquals(
                refreshToken,
                refreshed.get("refreshToken").asText());

        mockMvc.perform(post("/api/users/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "%s" }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutShouldRevokeCurrentAccessToken() throws Exception {
        String email = "logout-" + UUID.randomUUID() + "@finsight.local";
        String password = "StrongP@ss1";
        registerUser(email, password);

        JsonNode login = login(email, password);
        String accessToken = login.get("accessToken").asText();

        mockMvc.perform(post("/api/users/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAllShouldInvalidateExistingRefreshTokens() throws Exception {
        String email = "logout-all-" + UUID.randomUUID() + "@finsight.local";
        String password = "StrongP@ss1";
        registerUser(email, password);

        JsonNode login = login(email, password);
        String accessToken = login.get("accessToken").asText();
        String refreshToken = login.get("refreshToken").asText();

        mockMvc.perform(post("/api/users/auth/logout-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/users/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "%s" }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldRejectInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "demo@finsight.local",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldWorkForNewlyRegisteredUser() throws Exception {
        String email = "login-user-" + UUID.randomUUID() + "@finsight.local";
        String password = "AnotherP@ss1";

        registerUser(email, password);

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());
    }

    @Test
    void changePasswordShouldInvalidateOldPasswordAndAcceptNewPassword() throws Exception {
        String email = "change-pass-" + UUID.randomUUID() + "@finsight.local";
        String oldPassword = "StrongP@ss1";
        String newPassword = "NewStrongP@ss2";

        registerUser(email, oldPassword);
        JsonNode login = login(email, oldPassword);
        String accessToken = login.get("accessToken").asText();

        mockMvc.perform(post("/api/users/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "%s"
                                }
                                """.formatted(oldPassword, newPassword)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, oldPassword)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    private void registerUser(String email, String password) throws Exception {
        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isCreated());
    }

    private JsonNode login(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString());
    }

    private RSAPublicKey parsePublicKey(String publicKeyPem) throws Exception {
        String normalized = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }
}
