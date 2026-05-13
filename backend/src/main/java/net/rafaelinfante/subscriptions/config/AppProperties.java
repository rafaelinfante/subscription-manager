package net.rafaelinfante.subscriptions.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/** All app-specific configuration, grouped under the {@code app.*} namespace. */
@ConfigurationProperties("app")
public record AppProperties(
        Jwt jwt,
        Frontend frontend,
        Cors cors,
        Payment payment,
        Security security,
        OAuth2 oauth2) {

    public record Security(@DefaultValue("false") boolean cookieSecure) {
    }

    public record Jwt(
            @DefaultValue("subscription-manager") String issuer,
            @DefaultValue("PT15M") Duration accessTokenTtl,
            @DefaultValue("P7D") Duration refreshTokenTtl,
            @DefaultValue("") String rsaPublicKey,
            @DefaultValue("") String rsaPrivateKey) {
    }

    public record Frontend(
            @DefaultValue("http://localhost:4200") String baseUrl,
            @DefaultValue("/auth/oauth2-callback") String oauth2RedirectPath) {
    }

    public record Cors(@DefaultValue("http://localhost:4200") List<String> allowedOrigins) {
    }

    public record Payment(@DefaultValue("token") String mockMode) {
    }

    public record OAuth2(Registration google, Registration github, Registration facebook) {

        public record Registration(@DefaultValue("") String clientId, @DefaultValue("") String clientSecret) {
            public boolean isConfigured() {
                return clientId != null && !clientId.isBlank()
                        && clientSecret != null && !clientSecret.isBlank();
            }
        }
    }
}
