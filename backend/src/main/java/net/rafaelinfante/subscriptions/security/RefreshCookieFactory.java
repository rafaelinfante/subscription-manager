package net.rafaelinfante.subscriptions.security;

import net.rafaelinfante.subscriptions.config.AppProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Builds the HttpOnly cookie that carries the refresh token to the browser. */
@Component
public class RefreshCookieFactory {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String PATH = "/api/auth";

    private final boolean secure;
    private final Duration ttl;

    public RefreshCookieFactory(AppProperties properties) {
        this.secure = properties.security().cookieSecure();
        this.ttl = properties.jwt().refreshTokenTtl();
    }

    public ResponseCookie create(String token) {
        return base(token).maxAge(ttl).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(PATH);
    }
}
