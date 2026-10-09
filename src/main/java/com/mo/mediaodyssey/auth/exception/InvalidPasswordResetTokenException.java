package com.mo.mediaodyssey.auth.exception;

import org.springframework.security.authentication.AccountStatusException;

public class InvalidPasswordResetTokenException extends AccountStatusException {

    public InvalidPasswordResetTokenException(String msg) {
        super(msg);
    }
}
