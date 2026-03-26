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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
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
    void loginShouldReturnJwtForValidCredentials() throws Exception {
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
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = response.get("accessToken").asText();

        Claims claims = Jwts.parser()
                .verifyWith(parsePublicKey(PUBLIC_KEY_PEM))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        org.junit.jupiter.api.Assertions.assertEquals("demo@finsight.local", claims.getSubject());
        org.junit.jupiter.api.Assertions.assertEquals("finsight-user-service", claims.getIssuer());
        org.junit.jupiter.api.Assertions.assertEquals("USER", claims.get("role", String.class));
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

        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
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
