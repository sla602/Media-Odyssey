package com.mo.mediaodyssey.auth.security;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.mo.mediaodyssey.shared.model.User;

public class MOOAuth2UserPrincipal implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    public MOOAuth2UserPrincipal(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    @Override
    public String getName() {
        Object email = attributes.get("email");
        return email == null ? user.getEmail() : email.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof MOOAuth2UserPrincipal other) {
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

    @Override
    public int hashCode() {
        return Objects.hash(user.getEmail());
    }
}
