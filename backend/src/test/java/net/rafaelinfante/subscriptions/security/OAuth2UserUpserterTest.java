package net.rafaelinfante.subscriptions.security;

import net.rafaelinfante.subscriptions.domain.User;
import net.rafaelinfante.subscriptions.domain.enums.AuthProvider;
import net.rafaelinfante.subscriptions.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2UserUpserterTest {

    @Mock private UserRepository users;
    @InjectMocks private OAuth2UserUpserter upserter;

    @Test
    void createsANewAccountForAFreshSocialIdentity() {
        when(users.findByProviderAndProviderId(AuthProvider.GOOGLE, "g1")).thenReturn(Optional.empty());
        when(users.existsByEmail("new@example.com")).thenReturn(false);
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User user = upserter.upsert(AuthProvider.GOOGLE, "g1", "new@example.com", "New User");

        assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.getProviderId()).isEqualTo("g1");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getRoles()).contains("USER");
    }

    @Test
    void updatesTheProfileOfAReturningSocialUser() {
        User existing = new User();
        existing.setId(7L);
        existing.setProvider(AuthProvider.GITHUB);
        existing.setProviderId("gh1");
        when(users.findByProviderAndProviderId(AuthProvider.GITHUB, "gh1")).thenReturn(Optional.of(existing));
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User user = upserter.upsert(AuthProvider.GITHUB, "gh1", "gh@example.com", "GitHub User");

        assertThat(user.getId()).isEqualTo(7L);
        assertThat(user.getEmail()).isEqualTo("gh@example.com");
    }

    @Test
    void refusesToAdoptAnExistingAccountByEmail() {
        when(users.findByProviderAndProviderId(AuthProvider.GITHUB, "attacker")).thenReturn(Optional.empty());
        when(users.existsByEmail("victim@example.com")).thenReturn(true);
        lenient().when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> upserter.upsert(AuthProvider.GITHUB, "attacker", "victim@example.com", "Attacker"))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("already exists");
    }
}
