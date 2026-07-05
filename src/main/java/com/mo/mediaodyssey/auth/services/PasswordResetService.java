package com.mo.mediaodyssey.auth.services;

import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.mo.mediaodyssey.auth.config.AuthRoutes;
import com.mo.mediaodyssey.auth.dto.ForgotPasswordDto;
import com.mo.mediaodyssey.auth.dto.ResetPasswordDto;
import com.mo.mediaodyssey.auth.exception.InvalidPasswordResetTokenException;
import com.mo.mediaodyssey.auth.exception.PasswordResetNotAllowedException;
import com.mo.mediaodyssey.auth.model.PasswordResetToken;
import com.mo.mediaodyssey.auth.repository.PasswordResetTokenRepository;
import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.EmailService;

@Service
public class PasswordResetService {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${email.resettoken.expiry-in-minutes}")
    private int tokenExpiryInMinutes;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private AuthRateLimitService authRateLimitService;

    @Transactional
    public void requestPasswordReset(ForgotPasswordDto dto) {
        User user = userRepository.findByEmail(dto.email()).orElse(null);
        if (user == null) {
            return;
        }

        if (!user.isAccountNonLocked()) {
            throw new LockedException("User is locked. Cannot reset password for locked user. Contact support.");
        }

        if (user.isOauthAccount()) {
            throw buildProviderResetNotAllowed();
        }

        PasswordResetToken existingToken = user.getPasswordResetToken();
        if (existingToken != null) {
            user.setPasswordResetToken(null);
            passwordResetTokenRepository.delete(existingToken);
            passwordResetTokenRepository.flush();
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = new PasswordResetToken(token, user, tokenExpiryInMinutes);

        // Build the reset link from the request context. Tomcat resolves the
        // configured proxy headers so the URL uses the external scheme and host.
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();
        String passwordResetUrl = baseUrl + AuthRoutes.Page.RESET + "?token=" + token;

        String to = user.getEmail();
        String subject = "Reset Password for " + this.appName;
        String htmlMessage = emailService.buildAuthActionEmailHtml(
                this.appName,
                "Reset Password",
                "Reset your password",
                "We received a request to reset your password for " + this.appName + ".",
                "Reset Password",
                passwordResetUrl,
                tokenExpiryInMinutes,
                "If you did not request this, you can safely ignore this email.");

        emailService.sendHtmlEmail(to, subject, htmlMessage);
        passwordResetTokenRepository.save(passwordResetToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordDto dto) {
        PasswordResetToken tokenEntity = passwordResetTokenRepository.findByToken(dto.token())
                .filter(item -> item.getExpiryDate().after(new Date()))
                .orElseThrow(() -> new InvalidPasswordResetTokenException(
                        "Password reset token is invalid or expired. Please request a new reset link."));

        User user = tokenEntity.getUser();

        if (!user.isAccountNonLocked()) {
            throw new LockedException("User is locked. Cannot reset password for locked user. Contact support.");
        }

        if (user.isOauthAccount()) {
            throw buildProviderResetNotAllowed();
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setPasswordResetToken(null);
        userRepository.save(user);

        passwordResetTokenRepository.delete(tokenEntity);

        // A successful password reset should restore the caller's login and global
        // auth headroom so they can immediately try the new password.
        authRateLimitService.refundSuccessfulPasswordReset();

        expireUserSessions(user.getEmail());
    }

    private void expireUserSessions(String email) {
        for (SessionInformation session : sessionRegistry.getAllSessions(email, false)) {
            session.expireNow();
        }
    }

    private PasswordResetNotAllowedException buildProviderResetNotAllowed() {
        return new PasswordResetNotAllowedException(
                "Password reset is not available for provider-based sign-in accounts. Please sign in with the same provider.");
    }
}
