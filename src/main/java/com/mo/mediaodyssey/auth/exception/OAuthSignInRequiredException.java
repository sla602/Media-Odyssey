package com.mo.mediaodyssey.auth.exception;

import org.springframework.security.authentication.BadCredentialsException;

public class OAuthSignInRequiredException extends BadCredentialsException {

    public OAuthSignInRequiredException(String msg) {
        super(msg);
    }
}
