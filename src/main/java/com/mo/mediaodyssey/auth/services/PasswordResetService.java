package com.mo.mediaodyssey.auth.services;

import java.util.Locale;
import java.security.Principal;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

    @Value("${spring.application.name:App}")
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

    @Transactional
    public void requestPasswordReset(ForgotPasswordDto dto) {
        Optional<User> userOptional = userRepository.findByEmail(dto.email());

        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();

        if (user.isOauthAccount()) {
            throw buildOauthResetNotAllowed(user);
        }

        PasswordResetToken existingToken = user.getPasswordResetToken();
        if (existingToken != null) {
            user.setPasswordResetToken(null);
            passwordResetTokenRepository.delete(existingToken);
            passwordResetTokenRepository.flush();
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = new PasswordResetToken(token, user, tokenExpiryInMinutes);

        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();
        String passwordResetUrl = baseUrl + "/auth/reset?token=" + token;

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

        if (user.isOauthAccount()) {
            throw buildOauthResetNotAllowed(user);
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setPasswordResetToken(null);
        userRepository.save(user);

        passwordResetTokenRepository.delete(tokenEntity);

        expireUserSessions(user.getEmail());
    }

    private void expireUserSessions(String email) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (!matchesPrincipalEmail(principal, email)) {
                continue;
            }

            for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                session.expireNow();
            }
        }
    }

    private boolean matchesPrincipalEmail(Object principal, String email) {
        if (principal instanceof User userPrincipal) {
            return email.equalsIgnoreCase(userPrincipal.getEmail());
        }

        if (principal instanceof UserDetails userDetails) {
            return email.equalsIgnoreCase(userDetails.getUsername());
        }

        if (principal instanceof Principal principalDetails) {
            return email.equalsIgnoreCase(principalDetails.getName());
        }

        if (principal instanceof String principalName) {
            return email.equalsIgnoreCase(principalName);
        }

        return false;
    }

    private PasswordResetNotAllowedException buildOauthResetNotAllowed(User user) {
        String provider = user.getOauthProvider();

        if (provider == null || provider.isBlank()) {
            return new PasswordResetNotAllowedException(
                    "Password reset is not available for OAuth accounts. Please sign in with your OAuth provider.");
        }

        String providerLabel = provider.substring(0, 1).toUpperCase(Locale.ROOT)
            + provider.substring(1).toLowerCase(Locale.ROOT);
        return new PasswordResetNotAllowedException(
                "Password reset is not available for this account because it uses " + providerLabel
                        + " sign-in. Please continue with " + providerLabel + ".");
    }
}
