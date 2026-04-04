package com.mo.mediaodyssey.auth.services;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.auth.security.MOOAuth2UserPrincipal;
import com.mo.mediaodyssey.shared.model.User;

@Service
public class MOOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = delegate.loadUser(userRequest);
        String email = extractEmail(oauthUser.getAttributes());

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_email"),
                    "OAuth provider did not return an email.");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = createNewOauthUser(email, userRequest.getClientRegistration().getRegistrationId());
        } else if (!user.isOauthAccount()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_method_conflict"),
                    "This email is already registered with email/password.");
        }

        return new MOOAuth2UserPrincipal(user, oauthUser.getAttributes());
    }

    private User createNewOauthUser(String email, String registrationId) {
        // TODO: refactor as a constructor in User model, instead of setting values
        // after the constructor.
        // Password must remain non-null for the existing users table schema.
        User user = new User(email, passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(true);
        user.setRole("ROLE_USER");
        user.setAuthProvider("OAUTH");
        user.setOauthProvider(registrationId);
        return userRepository.save(user);
    }

    private String extractEmail(Map<String, Object> attributes) {
        Object email = attributes.get("email");
        return email == null ? null : email.toString().trim();
    }
}
