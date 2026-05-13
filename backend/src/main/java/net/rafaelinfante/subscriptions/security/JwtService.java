package net.rafaelinfante.subscriptions.security;

import net.rafaelinfante.subscriptions.config.AppProperties;
import net.rafaelinfante.subscriptions.domain.User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** Issues and decodes the application's own RS256 access and refresh tokens. */
@Service
public class JwtService {

    static final String TYPE_ACCESS = "access";
    static final String TYPE_REFRESH = "refresh";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final AppProperties.Jwt config;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder, AppProperties properties) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.config = properties.jwt();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(config.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(config.accessTokenTtl()))
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("name", user.getName())
                .claim("roles", List.copyOf(user.getRoles()))
                .claim("type", TYPE_ACCESS)
                .build();
        return encode(claims);
    }

    public String generateRefreshToken(User user, String jti, Instant expiresAt) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(jti)
                .issuer(config.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("type", TYPE_REFRESH)
                .build();
        return encode(claims);
    }

    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    public Instant refreshTokenExpiry(Instant from) {
        return from.plus(config.refreshTokenTtl());
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
