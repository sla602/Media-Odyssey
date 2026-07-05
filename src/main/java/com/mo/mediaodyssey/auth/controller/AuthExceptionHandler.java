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
import com.mo.mediaodyssey.auth.exception.InvalidPasswordResetTokenException;
import com.mo.mediaodyssey.auth.exception.InvalidVerificationTokenException;
import com.mo.mediaodyssey.auth.exception.OAuthSignInRequiredException;
import com.mo.mediaodyssey.auth.exception.PasswordResetNotAllowedException;
import com.mo.mediaodyssey.auth.exception.UserAlreadyVerifiedException;
import com.mo.mediaodyssey.dev.controller.DevAccountController;

/**
 * Exception handler for authentication-related exceptions.
 * 
 * This class provides centralized exception handling for the authentication
 * controllers, returning appropriate HTTP responses for various authentication
 * errors.
 */
@RestControllerAdvice(basePackageClasses = { AuthController.class, DevAccountController.class })
public class AuthExceptionHandler {

        /**
         * Handles OAuthSignInRequiredException thrown when a user attempts to sign in
         * with a username/password for an account that uses provider-based sign-in.
         *
         * @return ResponseEntity with error details and HTTP status 401 (Unauthorized).
         */
        @ExceptionHandler(OAuthSignInRequiredException.class)
        public ResponseEntity<AuthApiResponse> handleOauthSignInRequired() {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_OAUTH_SIGN_IN_REQUIRED",
                                                "This account uses provider-based sign-in. Please continue with the same provider."));
        }

        /**
         * Handles BadCredentialsException thrown during authentication.
         *
         * Unknown users and bad credentials intentionally share the same response
         * so auth endpoints do not reveal which accounts exist.
         *
         * @return ResponseEntity with error details and HTTP status 401 (Unauthorized).
         */
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<AuthApiResponse> handleBadCredentials() {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_INVALID_CREDENTIALS",
                                                "Invalid credentials. Please try again."));
        }

        /**
         * Handles DisabledException thrown when a user account is disabled.
         *
         * @return ResponseEntity with error details and HTTP status 403 (Forbidden).
         */
        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<AuthApiResponse> handleDisabled() {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_DISABLED",
                                                "User is disabled. Please contact support."));
        }

        /**
         * Handles LockedException thrown when a user account is locked.
         *
         * @return ResponseEntity with error details and HTTP status 403 (Forbidden).
         */
        @ExceptionHandler(LockedException.class)
        public ResponseEntity<AuthApiResponse> handleLocked() {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_LOCKED", "User is locked. Please contact support."));
        }

        /**
         * Handles UsernameNotFoundException thrown during authentication.
         *
         * Unknown users and bad credentials intentionally share the same response
         * so auth endpoints do not reveal which accounts exist.
         *
         * @return ResponseEntity with error details and HTTP status 401 (Unauthorized).
         */
        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<AuthApiResponse> handleUserNotFound() {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_INVALID_CREDENTIALS",
                                                "Invalid credentials. Please try again."));
        }

        /**
         * Handles UserAlreadyVerifiedException thrown when a user attempts to verify an
         * already verified account.
         *
         * @return ResponseEntity with error details and HTTP status 400 (Bad Request).
         */
        @ExceptionHandler(UserAlreadyVerifiedException.class)
        public ResponseEntity<AuthApiResponse> handleUserAlreadyVerified() {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_USER_ALREADY_VERIFIED",
                                                "User is already verified. Please log in."));
        }

        /**
         * Handles InvalidVerificationTokenException thrown when a user attempts to
         * verify an account with an invalid or expired verification token.
         *
         * @return ResponseEntity with error details and HTTP status 400 (Bad Request).
         */
        @ExceptionHandler(InvalidVerificationTokenException.class)
        public ResponseEntity<AuthApiResponse> handleInvalidVerificationToken() {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_INVALID_VERIFICATION_TOKEN",
                                                "Verification token is invalid. Please try again by requesting a new verification email. If you have previously followed this link to verify, please continue to log in."));
        }

        /**
         * Handles InvalidPasswordResetTokenException thrown when a user attempts to
         * reset their password with an invalid or expired token.
         *
         * @return ResponseEntity with error details and HTTP status 400 (Bad Request).
         */
        @ExceptionHandler(InvalidPasswordResetTokenException.class)
        public ResponseEntity<AuthApiResponse> handleInvalidPasswordResetToken() {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_INVALID_PASSWORD_RESET_TOKEN",
                                                "Password reset token is invalid or expired. Please request a new reset link."));
        }

        /**
         * Handles PasswordResetNotAllowedException thrown when a user attempts to reset
         * their password for an account that uses provider-based sign-in.
         *
         * @return ResponseEntity with error details and HTTP status 400 (Bad Request).
         */
        @ExceptionHandler(PasswordResetNotAllowedException.class)
        public ResponseEntity<AuthApiResponse> handlePasswordResetNotAllowed() {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_PASSWORD_RESET_OAUTH_NOT_ALLOWED",
                                                "Password reset is not available for provider-based sign-in accounts. Please sign in with the same provider. If you have forgotten your password, please reset it through the provider's account recovery process."));
        }

        /**
         * Handles various exceptions related to bad requests, such as validation errors
         * and missing parameters.
         *
         * @return ResponseEntity with error details and HTTP status 400 (Bad Request).
         */
        @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class,
                        IllegalArgumentException.class, MissingServletRequestParameterException.class })
        public ResponseEntity<AuthApiResponse> handleBadRequest() {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_BAD_REQUEST",
                                                "An unexpected error occurred because a bad authentication request was received. Please try again later."));
        }

        /**
         * Handles any unexpected exceptions that are not explicitly handled by other
         * exception handlers.
         *
         * @return ResponseEntity with error details and HTTP status 500 (Internal
         *         Server Error).
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<AuthApiResponse> handleUnknown() {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(AuthApiResponse.error("AUTH_INTERNAL_ERROR",
                                                "An unexpected authentication error occurred. Please try again later."));
        }
}
