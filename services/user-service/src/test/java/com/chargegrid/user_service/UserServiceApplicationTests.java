package com.chargegrid.user_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class UserServiceApplicationTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void contextLoads() {}

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void meProvisionsProfileFromAuthenticatedClaims() throws Exception {
        mockMvc.perform(get("/api/users/me").with(jwt("subject-1", "one@example.com", "One User")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.keycloakUserId").value("subject-1"))
                .andExpect(jsonPath("$.email").value("one@example.com"))
                .andExpect(jsonPath("$.displayName").value("One User"));
    }

    @Test
    void meUsesSubjectOwnershipAndDoesNotUseEmailToSelectProfile() throws Exception {
        mockMvc.perform(get("/api/users/me").with(jwt("subject-a", "a@example.com", "User A")))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(
                        get("/api/users/me")
                                .with(jwt("subject-a", "a-new@example.com", "Updated A")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.keycloakUserId").value("subject-a"))
                .andExpect(jsonPath("$.email").value("a-new@example.com"))
                .andExpect(jsonPath("$.displayName").value("Updated A"));
    }

    private RequestPostProcessor jwt(String subject, String email, String name) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(token -> token.subject(subject).claim("email", email).claim("name", name));
    }
}
