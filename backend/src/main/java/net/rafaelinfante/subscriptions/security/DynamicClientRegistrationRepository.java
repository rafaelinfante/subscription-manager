package net.rafaelinfante.subscriptions.security;

import net.rafaelinfante.subscriptions.config.AppProperties;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds OAuth2 client registrations from configuration, including only providers whose
 * credentials are present. This lets the application start with no social keys at all and
 * light up each provider as its credentials are supplied.
 */
@Component
public class DynamicClientRegistrationRepository
        implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final Map<String, ClientRegistration> registrations = new LinkedHashMap<>();

    public DynamicClientRegistrationRepository(AppProperties properties) {
        AppProperties.OAuth2 oauth2 = properties.oauth2();
        register("google", google(oauth2.google()));
        register("github", github(oauth2.github()));
        register("facebook", facebook(oauth2.facebook()));
    }

    private void register(String id, ClientRegistration registration) {
        if (registration != null) {
            registrations.put(id, registration);
        }
    }

    private ClientRegistration google(AppProperties.OAuth2.Registration reg) {
        if (reg == null || !reg.isConfigured()) {
            return null;
        }
        return CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(reg.clientId())
                .clientSecret(reg.clientSecret())
                .build();
    }

    private ClientRegistration github(AppProperties.OAuth2.Registration reg) {
        if (reg == null || !reg.isConfigured()) {
            return null;
        }
        return CommonOAuth2Provider.GITHUB.getBuilder("github")
                .clientId(reg.clientId())
                .clientSecret(reg.clientSecret())
                .scope("read:user", "user:email")
                .build();
    }

    private ClientRegistration facebook(AppProperties.OAuth2.Registration reg) {
        if (reg == null || !reg.isConfigured()) {
            return null;
        }
        return CommonOAuth2Provider.FACEBOOK.getBuilder("facebook")
                .clientId(reg.clientId())
                .clientSecret(reg.clientSecret())
                .scope("public_profile", "email")
                .userInfoUri("https://graph.facebook.com/me?fields=id,name,email")
                .userNameAttributeName("id")
                .build();
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return registrations.get(registrationId);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return registrations.values().iterator();
    }

    public boolean hasRegistrations() {
        return !registrations.isEmpty();
    }

    public Set<String> enabledProviderIds() {
        return registrations.keySet();
    }
}
