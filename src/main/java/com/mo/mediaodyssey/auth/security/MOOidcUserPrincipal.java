package com.mo.mediaodyssey.auth.security;

import java.security.Principal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.mo.mediaodyssey.shared.model.User;

/**
 * App principal backed by a local `User` and the OIDC claims.
 */
public class MOOidcUserPrincipal implements OidcUser, Principal {

    private final User user;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    /**
     * Creates a principal for a stored user and provider claims.
     */
    public MOOidcUserPrincipal(User user, OidcIdToken idToken, OidcUserInfo userInfo) {
        this.user = user;
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    /**
     * Returns the local user entity.
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns all claims exposed by the principal.
     */
    @Override
    public Map<String, Object> getAttributes() {
        return getClaims();
    }

    /**
     * Returns the merged ID token and user-info claims.
     */
    @Override
    public Map<String, Object> getClaims() {
        Map<String, Object> claims = new LinkedHashMap<>(idToken.getClaims());
        if (userInfo != null) {
            claims.putAll(userInfo.getClaims());
        }
        return claims;
    }

    /**
     * Returns the authorities from the stored `User`.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    /**
     * Returns the best stable name for the principal, which is the email when present.
     */
    @Override
    public String getName() {
        String email = getEmail();
        return email == null ? user.getEmail() : email;
    }

    /**
     * Returns the ID token received from the provider.
     */
    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

    /**
     * Returns provider user information when available.
     */
    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    /**
     * Compares principals and users by email.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof MOOidcUserPrincipal other) {
            return Objects.equals(this.user.getEmail(), other.user.getEmail());
        }

        if (obj instanceof User otherUser) {
            return Objects.equals(this.user.getEmail(), otherUser.getEmail());
        }

        if (obj instanceof Principal principal) {
            return Objects.equals(this.getName(), principal.getName());
        }

        return false;
    }

    /**
     * Hashes by email to match equality.
     */
    @Override
    public int hashCode() {
        return Objects.hash(user.getEmail());
    }
}
