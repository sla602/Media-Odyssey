package com.mo.mediaodyssey.auth.security;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.auth.services.MOUserDetailsService;

@Component
public class MOAuthenticationProvider implements AuthenticationProvider {

    // Inspiried by:
    // https://www.baeldung.com/spring-security-authentication-provider

    @Autowired
    private MOUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public @Nullable Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        UserDetails user = userDetailsService.loadUserByUsername(email);
        if (user instanceof User moUser && moUser.isOauthAccount()) {
            throw new BadCredentialsException("This account uses OAuth sign-in.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        if (!user.isEnabled()) {
            throw new DisabledException("Account not activated");
        }

        if (!user.isAccountNonLocked()) {
            throw new LockedException("Account is locked");
        }

        return new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

}