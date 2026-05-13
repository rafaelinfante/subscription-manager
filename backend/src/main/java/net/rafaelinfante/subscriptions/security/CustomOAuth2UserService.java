package net.rafaelinfante.subscriptions.security;

import net.rafaelinfante.subscriptions.domain.enums.AuthProvider;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Persists the local account behind a GitHub or Facebook sign-in. */
@Component
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2UserUpserter upserter;
    private final RestClient restClient = RestClient.create();

    public CustomOAuth2UserService(OAuth2UserUpserter upserter) {
        this.upserter = upserter;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));

        String providerId = user.getName();
        String name = stringAttribute(user, "name");
        String email = stringAttribute(user, "email");
        if (email == null && provider == AuthProvider.GITHUB) {
            email = fetchGithubPrimaryEmail(userRequest.getAccessToken().getTokenValue());
        }
        if (email == null) {
            email = providerId + "@" + registrationId + ".local";
        }

        upserter.upsert(provider, providerId, email, name);
        return user;
    }

    private String fetchGithubPrimaryEmail(String accessToken) {
        List<Map<String, Object>> emails = restClient.get()
                .uri("https://api.github.com/user/emails")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() {
                });
        if (emails == null) {
            return null;
        }
        return emails.stream()
                .filter(e -> Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified")))
                .map(e -> (String) e.get("email"))
                .findFirst()
                .orElse(null);
    }

    private static String stringAttribute(OAuth2User user, String key) {
        Object value = user.getAttributes().get(key);
        return value != null ? value.toString() : null;
    }
}
