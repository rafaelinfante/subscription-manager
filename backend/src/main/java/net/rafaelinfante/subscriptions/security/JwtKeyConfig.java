package net.rafaelinfante.subscriptions.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import net.rafaelinfante.subscriptions.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * Provides the RSA keypair used to sign and verify application JWTs. A PEM keypair can be
 * supplied through configuration for real deployments; otherwise an ephemeral keypair is
 * generated at startup, which keeps local runs zero-config.
 */
@Configuration
public class JwtKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfig.class);

    private final RSAKey rsaKey;

    public JwtKeyConfig(AppProperties properties) {
        AppProperties.Jwt jwt = properties.jwt();
        if (hasText(jwt.rsaPublicKey()) && hasText(jwt.rsaPrivateKey())) {
            this.rsaKey = fromPem(jwt.rsaPublicKey(), jwt.rsaPrivateKey());
            log.info("Loaded RSA signing key from configuration");
        } else {
            this.rsaKey = generateEphemeralKey();
            log.warn("No RSA key configured; generated an ephemeral keypair. "
                    + "Issued tokens will not survive a restart. Set app.jwt.rsa-* for stable signing.");
        }
    }

    @Bean
    JWKSource<SecurityContext> jwkSource() {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder() {
        try {
            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build JWT decoder", e);
        }
    }

    private static RSAKey generateEphemeralKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate RSA keypair", e);
        }
    }

    private static RSAKey fromPem(String publicPem, String privatePem) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                    new X509EncodedKeySpec(decode(publicPem)));
            RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(decode(privatePem)));
            return new RSAKey.Builder(publicKey).privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse RSA keypair from configuration", e);
        }
    }

    private static byte[] decode(String pem) {
        String body = pem.replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
