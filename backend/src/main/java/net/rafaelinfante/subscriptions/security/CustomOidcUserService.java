package net.rafaelinfante.subscriptions.security;

import net.rafaelinfante.subscriptions.domain.enums.AuthProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/** Persists the local account behind a Google (OIDC) sign-in. */
@Component
public class CustomOidcUserService extends OidcUserService {

    private final OAuth2UserUpserter upserter;

    public CustomOidcUserService(OAuth2UserUpserter upserter) {
        this.upserter = upserter;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser user = super.loadUser(userRequest);
        upserter.upsert(AuthProvider.GOOGLE, user.getName(), user.getEmail(), user.getFullName());
        return user;
    }
}
