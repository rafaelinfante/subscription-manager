package net.rafaelinfante.subscriptions.web.controller;

import jakarta.validation.Valid;
import net.rafaelinfante.subscriptions.security.AuthResult;
import net.rafaelinfante.subscriptions.security.AuthUser;
import net.rafaelinfante.subscriptions.security.CurrentUser;
import net.rafaelinfante.subscriptions.security.DynamicClientRegistrationRepository;
import net.rafaelinfante.subscriptions.security.RefreshCookieFactory;
import net.rafaelinfante.subscriptions.service.AuthService;
import net.rafaelinfante.subscriptions.web.dto.DtoMapper;
import net.rafaelinfante.subscriptions.web.dto.Dtos;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieFactory cookieFactory;
    private final DynamicClientRegistrationRepository clientRegistrations;

    public AuthController(AuthService authService, RefreshCookieFactory cookieFactory,
                          DynamicClientRegistrationRepository clientRegistrations) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
        this.clientRegistrations = clientRegistrations;
    }

    @PostMapping("/register")
    public ResponseEntity<Dtos.AuthResponse> register(@Valid @RequestBody Dtos.RegisterRequest request) {
        return authResponse(authService.register(request.email(), request.password(), request.name()),
                HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<Dtos.AuthResponse> login(@Valid @RequestBody Dtos.LoginRequest request) {
        return authResponse(authService.login(request.email(), request.password()), HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Dtos.AuthResponse> refresh(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        return authResponse(authService.refresh(refreshToken), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .build();
    }

    @GetMapping("/me")
    public Dtos.UserDto me(@CurrentUser AuthUser user) {
        return new Dtos.UserDto(user.id(), user.email(), user.name(), user.roles());
    }

    @GetMapping("/social-providers")
    public Dtos.SocialProvidersResponse socialProviders() {
        return new Dtos.SocialProvidersResponse(List.copyOf(clientRegistrations.enabledProviderIds()));
    }

    private ResponseEntity<Dtos.AuthResponse> authResponse(AuthResult result, HttpStatus status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(new Dtos.AuthResponse(result.accessToken(), DtoMapper.toUserDto(result.user())));
    }
}
