package com.mo.mediaodyssey.auth.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.auth.security.MOOidcUserPrincipal;
import com.mo.mediaodyssey.shared.model.User;

/**
 * Loads or creates the local `User` for an OIDC sign-in.
 */
@Service
public class MOOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Reads the OIDC email claim, resolves the local account, and returns the app
     * principal.
     */
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcIdToken idToken = userRequest.getIdToken();
        String email = extractEmail(idToken);

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_email"),
                    "OIDC provider did not return an email.");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = createNewOidcUser(email, userRequest.getClientRegistration().getRegistrationId(),
                    idToken.getSubject());
        } else if (!user.isOauthAccount()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_method_conflict"),
                    "This email is already registered with email/password.");
        }

        return new MOOidcUserPrincipal(user, idToken, null);
    }

    /**
     * Creates the local account record for a first-time OIDC sign-in and stores
     * the provider registration id with the provider subject.
     */
    private User createNewOidcUser(String email, String registrationId, String providerUserId) {
        User user = new User(email, passwordEncoder.encode(UUID.randomUUID().toString()), registrationId,
                providerUserId);
        return userRepository.save(user);
    }

    /**
     * Extracts the email claim from the OIDC ID token.
     */
    private String extractEmail(OidcIdToken idToken) {
        String email = idToken.getEmail();
        return email == null ? null : email.trim();
    }
}
