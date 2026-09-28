package com.rgv.tradingbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-flow;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "spring.sql.init.mode=never",
        "trading.scheduler.enabled=false"
})
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    void registrationCreatesIsolatedAccountsAndEnforcesAdminRole() throws Exception {
        Csrf csrf = getCsrf();
        MvcResult adminResult = register(
                csrf, null, "Workspace Owner", "owner@example.com", "secure-password");
        JsonNode admin = objectMapper.readTree(adminResult.getResponse().getContentAsString());
        MockHttpSession adminSession = (MockHttpSession) adminResult.getRequest().getSession(false);

        assertEquals("ADMIN", admin.path("role").asText());
        assertEquals(1L, admin.path("portfolioId").asLong());

        MvcResult userResult = register(
                csrf, null, "Workspace Member", "member@example.com", "member-password");
        JsonNode user = objectMapper.readTree(userResult.getResponse().getContentAsString());
        MockHttpSession userSession = (MockHttpSession) userResult.getRequest().getSession(false);
        long userPortfolio = user.path("portfolioId").asLong();

        assertEquals("USER", user.path("role").asText());
        assertEquals(2L, userPortfolio);

        String loginBody = objectMapper.writeValueAsString(
                new AuthController.LoginRequest("member@example.com", "member-password"));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .session(userSession)
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode signedInUser = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        MockHttpSession authenticatedUserSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertEquals("USER", signedInUser.path("role").asText());

        mockMvc.perform(get("/api/portfolio/{id}/summary", userPortfolio).session(authenticatedUserSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/portfolio/{id}/summary", admin.path("portfolioId").asLong()).session(authenticatedUserSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/users").session(authenticatedUserSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").session(adminSession))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .session(authenticatedUserSession))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/portfolio/{id}/summary", userPortfolio).session(authenticatedUserSession))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserReturnsNoContentWhenSignedOut() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isNoContent());
    }

    @Test
    void corsAllowsPaperOrderCancellationPreflight() throws Exception {
        mockMvc.perform(options("/api/trades/orders/1")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "DELETE")
                        .header("Access-Control-Request-Headers", "x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("DELETE")));
    }

    private Csrf getCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
        return new Csrf(result.getResponse().getCookie("XSRF-TOKEN"), token);
    }

    private MvcResult register(
            Csrf csrf, MockHttpSession session, String name, String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new AuthController.RegistrationRequest(name, email, password));
        var request = post("/api/auth/register")
                .cookie(csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token())
                .contentType("application/json")
                .content(body);
        if (session != null) request.with(session(session));
        return mockMvc.perform(request).andExpect(status().isCreated()).andReturn();
    }

    private RequestPostProcessor session(MockHttpSession session) {
        return request -> {
            request.setSession(session);
            return request;
        };
    }

    private record Csrf(Cookie cookie, String token) {}
}
