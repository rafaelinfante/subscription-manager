package net.rafaelinfante.subscriptions.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.rafaelinfante.subscriptions.config.AppProperties;
import net.rafaelinfante.subscriptions.domain.User;
import net.rafaelinfante.subscriptions.domain.enums.AuthProvider;
import net.rafaelinfante.subscriptions.repository.UserRepository;
import net.rafaelinfante.subscriptions.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;

/**
 * After a social login completes, mints the application's own tokens for the linked account,
 * sets the refresh cookie, and hands the browser back to the SPA, which then exchanges the
 * cookie for an access token.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository users;
    private final AuthService authService;
    private final AppProperties.Frontend frontend;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public OAuth2LoginSuccessHandler(UserRepository users, AuthService authService, AppProperties properties) {
        this.users = users;
        this.authService = authService;
        this.frontend = properties.frontend();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        AuthProvider provider = AuthProvider.valueOf(token.getAuthorizedClientRegistrationId().toUpperCase(Locale.ROOT));
        User user = users.findByProviderAndProviderId(provider, token.getName())
                .orElseThrow(() -> new IllegalStateException("Social user was not persisted"));

        AuthResult result = authService.issueTokens(user);
        response.addHeader(HttpHeaders.SET_COOKIE, result.refreshCookie().toString());

        String target = UriComponentsBuilder.fromUriString(frontend.baseUrl())
                .path(frontend.oauth2RedirectPath())
                .queryParam("status", "success")
                .build().toUriString();
        redirectStrategy.sendRedirect(request, response, target);
    }
}
