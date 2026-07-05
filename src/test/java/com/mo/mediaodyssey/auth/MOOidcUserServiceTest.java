package com.mo.mediaodyssey.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.auth.security.MOOidcUserPrincipal;
import com.mo.mediaodyssey.auth.services.MOOidcUserService;
import com.mo.mediaodyssey.shared.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MOOidcUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MOOidcUserService service;

    @Test
    void loadUser_withExistingOidcUser_reusesStoredUserByOidcEmail() {
        String email = "oauth-user@example.com";
        String subject = "google-subject-123";
        User user = oauthUser(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        OidcUser result = service.loadUser(oidcUserRequest(email, subject));

        assertThat(result).isInstanceOf(MOOidcUserPrincipal.class);
        MOOidcUserPrincipal principal = (MOOidcUserPrincipal) result;
        assertThat(principal.getUser()).isSameAs(user);
        assertThat(principal.getEmail()).isEqualTo(email);
        assertThat(principal.getName()).isEqualTo(email);
        assertThat(principal.getIdToken().getSubject()).isEqualTo(subject);
        assertThat(principal).isInstanceOf(Principal.class);
        assertThat(user).isEqualTo(principal);
        verify(userRepository).findByEmail(email);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void loadUser_withLocalAccount_rejectsOidcLogin() {
        String email = "local-user@example.com";
        User user = new User(email, "encoded-password");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.loadUser(oidcUserRequest(email, "subject-1")))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("already registered");

        verify(userRepository).findByEmail(email);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void loadUser_withMissingEmail_rejectsOidcLogin() {
        OidcUserRequest request = oidcUserRequest(null, "subject-1");

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("did not return an email");

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void loadUser_withNewOidcUserCreatesAndPersistsAccount() {
        String email = "new-user@example.com";
        String subject = "google-subject-456";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OidcUser result = service.loadUser(oidcUserRequest(email, subject));

        assertThat(result).isInstanceOf(MOOidcUserPrincipal.class);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.isEmailVerified()).isTrue();
        assertThat(savedUser.getRole()).isEqualTo("ROLE_USER");
        assertThat(savedUser.getAuthProvider()).isEqualTo("OAUTH");
        assertThat(savedUser.getOauthProvider()).isEqualTo("google");
        assertThat(savedUser.getOauthProviderUserId()).isEqualTo(subject);
    }

    private User oauthUser(String email) {
        User user = new User(email, "encoded-password");
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setAuthProvider("OAUTH");
        user.setOauthProvider("google");
        user.setOauthProviderUserId("google-subject-123");
        return user;
    }

    private OidcUserRequest oidcUserRequest(String email, String subject) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/auth/oauth2/callback/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .scope("openid", "profile", "email")
                .clientName("Google")
                .build();

        Instant now = Instant.now();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(TokenType.BEARER, "access-token", now,
                now.plusSeconds(3600));
        OidcIdToken idToken = new OidcIdToken("id-token", now, now.plusSeconds(3600),
                buildClaims(email, subject));

        return new OidcUserRequest(registration, accessToken, idToken);
    }

    private Map<String, Object> buildClaims(String email, String subject) {
        Map<String, Object> claims = new java.util.LinkedHashMap<>();
        claims.put("sub", subject);
        if (email != null) {
            claims.put("email", email);
        }
        return claims;
    }
}
