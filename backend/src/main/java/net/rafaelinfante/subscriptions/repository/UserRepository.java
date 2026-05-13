package net.rafaelinfante.subscriptions.repository;

import net.rafaelinfante.subscriptions.domain.User;
import net.rafaelinfante.subscriptions.domain.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
