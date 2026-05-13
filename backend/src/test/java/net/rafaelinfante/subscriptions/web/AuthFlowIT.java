package net.rafaelinfante.subscriptions.web;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import net.rafaelinfante.subscriptions.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void registerIssuesATokenThatUnlocksMe() throws Exception {
        String response = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"newuser@example.com","password":"Password123!","name":"New User"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("newuser@example.com"))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(response, "$.accessToken");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void seededDemoUserCanLogIn() throws Exception {
        login("demo@demo.io", "Password123!")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("demo@demo.io"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        login("demo@demo.io", "not-the-password").andExpect(status().isUnauthorized());
    }

    @Test
    void registeringAnExistingEmailConflicts() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo@demo.io","password":"Password123!","name":"Duplicate"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_taken"));
    }

    @Test
    void refreshCookieYieldsANewAccessToken() throws Exception {
        Cookie refreshCookie = login("demo@demo.io", "Password123!")
                .andReturn().getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();

        mvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        mvc.perform(get("/api/subscriptions")).andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedFailedLoginsAreRateLimited() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            login("bruteforce@example.com", "wrong-password").andExpect(status().isUnauthorized());
        }
        login("bruteforce@example.com", "wrong-password")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("too_many_requests"));
    }

    @Test
    void noSocialProvidersAreEnabledWithoutCredentials() throws Exception {
        mvc.perform(get("/api/auth/social-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers").isEmpty());
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password)));
    }
}
