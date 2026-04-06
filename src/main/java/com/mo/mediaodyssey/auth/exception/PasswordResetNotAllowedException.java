package com.mo.mediaodyssey.auth.exception;

import org.springframework.security.authentication.AccountStatusException;

public class PasswordResetNotAllowedException extends AccountStatusException {

    public PasswordResetNotAllowedException(String msg) {
        super(msg);
    }
}
