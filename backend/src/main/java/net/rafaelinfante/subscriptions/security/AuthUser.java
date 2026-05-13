package net.rafaelinfante.subscriptions.security;

import java.util.Set;

/** The authenticated caller, resolved from the access-token claims. */
public record AuthUser(Long id, String email, String name, Set<String> roles) {

    public boolean isAdmin() {
        return roles.contains("ADMIN");
    }
}
