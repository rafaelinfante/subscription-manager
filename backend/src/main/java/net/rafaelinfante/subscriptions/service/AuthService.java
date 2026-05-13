package net.rafaelinfante.subscriptions.service;

import net.rafaelinfante.subscriptions.domain.RefreshToken;
import net.rafaelinfante.subscriptions.domain.User;
import net.rafaelinfante.subscriptions.domain.enums.AuthProvider;
import net.rafaelinfante.subscriptions.repository.RefreshTokenRepository;
import net.rafaelinfante.subscriptions.repository.UserRepository;
import net.rafaelinfante.subscriptions.security.AuthResult;
import net.rafaelinfante.subscriptions.security.JwtService;
import net.rafaelinfante.subscriptions.security.LoginAttemptService;
import net.rafaelinfante.subscriptions.security.RefreshCookieFactory;
import net.rafaelinfante.subscriptions.web.advice.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshCookieFactory cookieFactory;
    private final LoginAttemptService loginAttempts;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       RefreshCookieFactory cookieFactory, LoginAttemptService loginAttempts) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieFactory = cookieFactory;
        this.loginAttempts = loginAttempts;
    }

    @Transactional
    public AuthResult register(String email, String rawPassword, String name) {
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("email_taken", "An account with this email already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setProvider(AuthProvider.LOCAL);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRoles(new java.util.HashSet<>(Set.of("USER")));
        return issueTokens(users.save(user));
    }

    @Transactional
    public AuthResult login(String email, String rawPassword) {
        loginAttempts.assertNotLocked(email);
        User user = users.findByEmail(email)
                .filter(u -> u.getPasswordHash() != null)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElse(null);
        if (user == null) {
            loginAttempts.recordFailure(email);
            throw ApiException.unauthorized("Invalid email or password");
        }
        loginAttempts.recordSuccess(email);
        return issueTokens(user);
    }

    @Transactional
    public AuthResult refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw ApiException.unauthorized("Missing refresh token");
        }
        Jwt jwt = decodeRefresh(refreshTokenValue);
        RefreshToken stored = refreshTokens.findByJti(jwt.getId())
                .filter(t -> !t.isRevoked())
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> ApiException.unauthorized("Refresh token is no longer valid"));

        stored.setRevoked(true);
        refreshTokens.save(stored);
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }
        try {
            Jwt jwt = decodeRefresh(refreshTokenValue);
            refreshTokens.findByJti(jwt.getId()).ifPresent(t -> {
                t.setRevoked(true);
                refreshTokens.save(t);
            });
        } catch (ApiException ignored) {
            // A malformed cookie on logout is harmless; the client clears it regardless.
        }
    }

    public AuthResult issueTokens(User user) {
        String jti = UUID.randomUUID().toString();
        Instant expiresAt = jwtService.refreshTokenExpiry(Instant.now());
        refreshTokens.save(new RefreshToken(jti, user, expiresAt));

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, jti, expiresAt);
        return new AuthResult(accessToken, cookieFactory.create(refreshToken), user);
    }

    private Jwt decodeRefresh(String token) {
        Jwt jwt;
        try {
            jwt = jwtService.decode(token);
        } catch (JwtException e) {
            throw ApiException.unauthorized("Invalid refresh token");
        }
        if (!"refresh".equals(jwt.getClaimAsString("type"))) {
            throw ApiException.unauthorized("Token is not a refresh token");
        }
        return jwt;
    }
}
