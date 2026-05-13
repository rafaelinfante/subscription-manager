package net.rafaelinfante.subscriptions.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import net.rafaelinfante.subscriptions.config.AppProperties;
import net.rafaelinfante.subscriptions.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID("test")
                .build();

        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        var decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        var jwt = new AppProperties.Jwt("subscription-manager", Duration.ofMinutes(15), Duration.ofDays(7), "", "");
        var props = new AppProperties(jwt, null, null, null, null, null);

        jwtService = new JwtService(encoder, decoder, props);
    }

    @Test
    void accessTokenCarriesIdentityAndRoles() {
        User user = user();

        Jwt decoded = jwtService.decode(jwtService.generateAccessToken(user));

        assertThat(decoded.getSubject()).isEqualTo("alice@example.com");
        assertThat(decoded.getClaimAsString("type")).isEqualTo("access");
        assertThat(((Number) decoded.getClaim("uid")).longValue()).isEqualTo(42L);
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly("USER");
    }

    @Test
    void refreshTokenCarriesItsId() {
        Jwt decoded = jwtService.decode(
                jwtService.generateRefreshToken(user(), "jti-1", Instant.now().plusSeconds(3600)));

        assertThat(decoded.getId()).isEqualTo("jti-1");
        assertThat(decoded.getClaimAsString("type")).isEqualTo("refresh");
    }

    private static User user() {
        User user = new User();
        user.setId(42L);
        user.setEmail("alice@example.com");
        user.setName("Alice");
        user.setRoles(Set.of("USER"));
        return user;
    }
}
