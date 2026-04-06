package com.mo.mediaodyssey.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mo.mediaodyssey.auth.dto.AuthApiResponse;
import com.mo.mediaodyssey.auth.exception.InvalidVerificationTokenException;
import com.mo.mediaodyssey.auth.exception.OAuthSignInRequiredException;
import com.mo.mediaodyssey.auth.exception.UserAlreadyVerifiedException;
import com.mo.mediaodyssey.dev.controller.DevAccountController;

@RestControllerAdvice(basePackageClasses = { AuthController.class, DevAccountController.class })
public class AuthExceptionHandler {

        // Debugging assisted by AI.

        @ExceptionHandler(OAuthSignInRequiredException.class)
        public ResponseEntity<AuthApiResponse> handleOauthSignInRequired() {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_OAUTH_SIGN_IN_REQUIRED",
                                                "This account uses OAuth sign-in. Please continue via OAuth provider."));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<AuthApiResponse> handleBadCredentials() {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_INVALID_CREDENTIALS",
                                                "Invalid credentials. Please try again."));
        }

        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<AuthApiResponse> handleDisabled() {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_DISABLED",
                                                "User is disabled. Please contact support."));
        }

        @ExceptionHandler(LockedException.class)
        public ResponseEntity<AuthApiResponse> handleLocked() {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_LOCKED", "User is locked. Please contact support."));
        }

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<AuthApiResponse> handleUserNotFound() {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_USER_NOT_FOUND",
                                                "User could not be found. Please try again."));
        }

        @ExceptionHandler(UserAlreadyVerifiedException.class)
        public ResponseEntity<AuthApiResponse> handleUserAlreadyVerified() {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_USER_ALREADY_VERIFIED",
                                                "User is already verified. Please log in."));
        }

        @ExceptionHandler(InvalidVerificationTokenException.class)
        public ResponseEntity<AuthApiResponse> handleInvalidVerificationToken() {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_INVALID_VERIFICATION_TOKEN",
                                                "Verification token is invalid. If you have previously followed this link to verify, please continue to log in. Please try again."));
        }

        @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class,
                        IllegalArgumentException.class, MissingServletRequestParameterException.class })
        public ResponseEntity<AuthApiResponse> handleBadRequest() {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_BAD_REQUEST",
                                                "An unexpected error occurred because a bad authentication request was received. Please try again later."));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<AuthApiResponse> handleUnknown() {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_INTERNAL_ERROR",
                                                "An unexpected authentication error occurred. Please try again later."));
        }
}
