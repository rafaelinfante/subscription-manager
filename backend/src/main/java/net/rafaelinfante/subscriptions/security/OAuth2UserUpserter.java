package net.rafaelinfante.subscriptions.security;

import net.rafaelinfante.subscriptions.domain.User;
import net.rafaelinfante.subscriptions.domain.enums.AuthProvider;
import net.rafaelinfante.subscriptions.repository.UserRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/** Maps a social identity onto a local user, creating or linking the account as needed. */
@Component
public class OAuth2UserUpserter {

    private final UserRepository users;

    public OAuth2UserUpserter(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public User upsert(AuthProvider provider, String providerId, String email, String name) {
        return users.findByProviderAndProviderId(provider, providerId)
                .map(existing -> updateProfile(existing, email, name))
                .orElseGet(() -> createLinkedAccount(provider, providerId, email, name));
    }

    private User createLinkedAccount(AuthProvider provider, String providerId, String email, String name) {
        // Never adopt an existing account just because the email matches — that would let a social
        // identity take over someone else's account. A clashing email fails the sign-in instead.
        if (users.existsByEmail(email)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "email_taken", "An account with this email already exists; sign in with that method", null));
        }
        User user = new User();
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setRoles(new HashSet<>(Set.of("USER")));
        return updateProfile(user, email, name);
    }

    private User updateProfile(User user, String email, String name) {
        user.setEmail(email);
        if (name != null && !name.isBlank()) {
            user.setName(name);
        }
        return users.save(user);
    }
}
