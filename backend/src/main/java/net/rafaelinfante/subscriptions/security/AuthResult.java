package net.rafaelinfante.subscriptions.security;

import net.rafaelinfante.subscriptions.domain.User;
import org.springframework.http.ResponseCookie;

/** The outcome of an authentication: an access token for the body and a refresh cookie. */
public record AuthResult(String accessToken, ResponseCookie refreshCookie, User user) {
}
