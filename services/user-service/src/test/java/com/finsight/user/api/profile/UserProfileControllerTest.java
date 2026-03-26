package com.finsight.user.api.profile;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:userdb-profile;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAndUpdateProfileShouldWorkForAuthenticatedUser() throws Exception {
        String email = "profile-" + UUID.randomUUID() + "@finsight.local";
        registerUser(email, "StrongP@ss1");

        mockMvc.perform(get("/api/users/me")
                        .with(jwtFor(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(get("/api/users/me/security")
                        .with(jwtFor(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.profileConfigured").value(false));

        mockMvc.perform(put("/api/users/me")
                        .with(jwtFor(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Aayan Singh",
                                  "city": "Jaipur",
                                  "ageGroup": "18-24",
                                  "monthlyIncome": 75000.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Aayan Singh"))
                .andExpect(jsonPath("$.city").value("Jaipur"))
                .andExpect(jsonPath("$.ageGroup").value("18-24"));

        mockMvc.perform(get("/api/users/me/security")
                        .with(jwtFor(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileConfigured").value(true));
    }

    @Test
    void deleteAccountShouldRemoveUser() throws Exception {
        String email = "delete-" + UUID.randomUUID() + "@finsight.local";
        registerUser(email, "StrongP@ss1");

        mockMvc.perform(delete("/api/users/me")
                        .with(jwtFor(email)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me")
                        .with(jwtFor(email)))
                .andExpect(status().isNotFound());
    }

    @Test
    void profileEndpointsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me/security"))
                .andExpect(status().isUnauthorized());
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

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtFor(String email) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(email));
    }
}
