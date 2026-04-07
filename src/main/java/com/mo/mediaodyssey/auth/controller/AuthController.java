package com.mo.mediaodyssey.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mo.mediaodyssey.auth.dto.ResendVerifyTokenDto;
import com.mo.mediaodyssey.auth.dto.AuthApiResponse;
import com.mo.mediaodyssey.auth.dto.ForgotPasswordDto;
import com.mo.mediaodyssey.auth.dto.LoginDto;
import com.mo.mediaodyssey.auth.dto.ResetPasswordDto;
import com.mo.mediaodyssey.auth.dto.UserDto;
import com.mo.mediaodyssey.auth.dto.VerifyTokenDto;
import com.mo.mediaodyssey.auth.services.MOLocalAuthService;
import com.mo.mediaodyssey.auth.services.EmailVerificationService;
import com.mo.mediaodyssey.auth.services.PasswordResetService;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Inspired by:
    // https://www.baeldung.com/spring-security-authentication-provider
    // https://www.djamware.com/post/secure-your-restful-api-with-spring-boot-35-jwt-and-mongodb
    // Debugging assisted by AI.

    @Autowired
    private MOLocalAuthService authService;

    @Autowired
    private EmailVerificationService verificationService;

    @Autowired
    private SessionAuthenticationStrategy sessionAuthenticationStrategy;

    @Autowired
    private CurrentAccountService currentAccountService;

    @Autowired
    private TokenBasedRememberMeServices rememberMeServices;

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * Handles account login requests.
     *
     * @param dto      The login request data containing credentials.
     * @param request  The HTTP request.
     * @param response The HTTP response.
     * @return The login response containing success status and message. Session is
     *         managed by Spring Security, and the security context is stored in the
     *         session upon successful authentication. Otherwise, the error status
     *         and message is returned. Authentication exceptions are handled by
     *         AuthExceptionHandler.
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<AuthApiResponse> login(@Valid @RequestBody LoginDto dto, HttpServletRequest request,
            HttpServletResponse response) {
        // Login the User
        Authentication authentication = authService.loginUser(dto);

        // Apply session concurrency + session fixation strategy for custom login flow.
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

        // Persist the login using the same principal refresh flow shared by both local
        // and OAuth login.
        currentAccountService.refreshPrincipal(authentication, request, response);

        // Remember-me is login-only and opt-in.
        if (dto.rememberMeRequested()) {
            rememberMeServices.onLoginSuccess(request, response, authentication);
        } else {
            rememberMeServices.loginFail(request, response);
        }

        // Return OK - successfully logged in
        if (authentication.getPrincipal() instanceof User user && !user.isEmailVerified()) {
            return ResponseEntity.ok(AuthApiResponse.success("AUTH_LOGIN_SUCCESS_UNVERIFIED",
                    "Login successful. Please verify your email."));
        }

        return ResponseEntity.ok(AuthApiResponse.success("AUTH_LOGIN_SUCCESS", "Login successful"));
    }

    /**
     * Handles account registration requests.
     *
     * @param dto The registration request data containing user details.
     * @return The registration response containing success status and message.
     *         Session is not created upon registration; the user must log in after
     *         verifying their email. Otherwise, the error status and message is
     *         returned. Authentication exceptions are handled by
     *         AuthExceptionHandler.
     */
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<AuthApiResponse> register(@Valid @RequestBody UserDto dto) {
        authService.registerUser(dto);

        // Return OK - successfully registered
        return ResponseEntity.ok(AuthApiResponse.success("AUTH_REGISTER_SUCCESS", "Registration successful"));
    }

    /**
     * Handles completing email verification requests.
     *
     * @param token The verification token.
     * @return The verification response containing success status and message.
     *         Authentication exceptions are handled by AuthExceptionHandler.
     */
    @PostMapping(value = "/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<AuthApiResponse> verify(@Valid @RequestParam("token") String token) {
        VerifyTokenDto dto = new VerifyTokenDto(token);
        verificationService.verifyUser(dto);

        return ResponseEntity.ok(
                AuthApiResponse.success("AUTH_VERIFY_SUCCESS", "Email verified successfully. You can now log in."));
    }

    /**
     * Handles resending email verification token requests.
     *
     * @param dto The resend request data containing user details.
     * @return The resend response containing success status and message. Otherwise,
     *         the error status and message is returned. Authentication exceptions
     *         are handled by AuthExceptionHandler.
     */
    @PostMapping(value = "/resend", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<AuthApiResponse> resend(@Valid @RequestBody ResendVerifyTokenDto dto) {
        verificationService.resendVerification(dto);

        // Return OK - successfully resent
        return ResponseEntity.ok(AuthApiResponse.success("AUTH_RESEND_SUCCESS", "Verification email resent"));
    }

    /**
     * Handles password reset request submissions.
     *
     * @param dto The request data containing user email.
     * @return Generic response to avoid leaking account existence. For eligible
     *         local accounts, a reset email is sent.
     */
    @PostMapping(value = "/password/forgot", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<AuthApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordDto dto) {
        passwordResetService.requestPasswordReset(dto);

        return ResponseEntity.ok(AuthApiResponse.success("AUTH_PASSWORD_RESET_EMAIL_SENT",
                "If an eligible account exists, a password reset link has been sent."));
    }

    /**
     * Handles password reset completion submissions.
     *
     * @param dto The reset data containing token and new password.
     * @return Successful reset response when token is valid and not expired.
     */
    @PostMapping(value = "/password/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<AuthApiResponse> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        passwordResetService.resetPassword(dto);

        return ResponseEntity.ok(AuthApiResponse.success("AUTH_PASSWORD_RESET_SUCCESS",
                "Password reset successful. You can now log in with your new password."));
    }
}
