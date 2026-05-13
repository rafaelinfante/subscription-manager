package net.rafaelinfante.subscriptions.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.rafaelinfante.subscriptions.config.AppProperties;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final AppProperties.Frontend frontend;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public OAuth2LoginFailureHandler(AppProperties properties) {
        this.frontend = properties.frontend();
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String target = UriComponentsBuilder.fromUriString(frontend.baseUrl())
                .path(frontend.oauth2RedirectPath())
                .queryParam("status", "error")
                .build().toUriString();
        redirectStrategy.sendRedirect(request, response, target);
    }
}
