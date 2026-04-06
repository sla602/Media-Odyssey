package com.mo.mediaodyssey.auth.services;

import java.util.Date;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.mo.mediaodyssey.auth.model.VerificationToken;
import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.auth.repository.VerificationTokenRepository;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.EmailService;
import com.mo.mediaodyssey.auth.dto.ResendVerifyTokenDto;
import com.mo.mediaodyssey.auth.dto.VerifyTokenDto;
import com.mo.mediaodyssey.auth.exception.InvalidVerificationTokenException;
import com.mo.mediaodyssey.auth.exception.UserAlreadyVerifiedException;

@Service
public class EmailVerificationService {

    // Inspiried by: https://www.baeldung.com/registration-verify-user-by-email
    // Debugging assisted by AI

    // Expiry length determined by environment variable. Default: 60 minutes
    @Value("${spring.application.name:App}")
    private String appName;

    @Value("${email.verifytoken.expiry-in-minutes}")
    private int tokenExpiryInMinutes;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public void createVerification(User user) {
        // Create verification token
        String token = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken(token, user, tokenExpiryInMinutes);

        // Build verification token email
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();
        String verificationUrl = baseUrl + "/auth/verify?token=" + token;
        String to = user.getEmail();
        String subject = "Confirm Registration to " + this.appName;
        String htmlMessage = buildVerificationEmailHtml(verificationUrl);

        // Send verification token email
        emailService.sendHtmlEmail(to, subject, htmlMessage);

        // Save verification token
        verificationTokenRepository.save(vt);
    }

    @Transactional
    public void verifyUser(VerifyTokenDto dto) {
        try {
            // Find the User if the token is valid and not expired.
            String token = dto.token();
            VerificationToken tokenEntity = verificationTokenRepository.findByToken(token)
                    .filter(item -> item.getExpiryDate().after(new Date())).orElseThrow();
            User user = tokenEntity.getUser();

            // Mark the user's email verification state and save this change.
            user.setEmailVerified(true);
            userRepository.save(user);

            // Delete the verification token because it is no longer needed.
            user.setVerificationToken(null);
            verificationTokenRepository.delete(tokenEntity);
        } catch (NoSuchElementException e) {
            throw new InvalidVerificationTokenException("Verification token is invalid. Please try again.");
        }
    }

    @Transactional
    public void resendVerification(ResendVerifyTokenDto dto) {
        try {
            User user = userRepository.findByEmail(dto.email()).orElseThrow();
            if (user.isAccountNonLocked() == false) {
                throw new LockedException("User is locked. Cannot verify locked user. Contact support.");
            } else if (user.isEmailVerified()) { // If User is already email verified.
                throw new UserAlreadyVerifiedException("User is already verified. Please log in.");
            } else {
                VerificationToken tokenEntity = user.getVerificationToken();
                if (tokenEntity != null) { // If previous token exist, delete it. If not, skip deleting.
                    user.setVerificationToken(null);
                    verificationTokenRepository.delete(tokenEntity);
                    verificationTokenRepository.flush();
                }
                this.createVerification(user);
            }
        } catch (NoSuchElementException e) {
            throw new UsernameNotFoundException("User not found with email: " + dto.email());
        }

    }

    private String buildVerificationEmailHtml(String verificationUrl) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s - Verify Your Email</title>
                </head>
                <body style="margin:0; padding:0; background:#101322; color:#f5f7ff; font-family:Verdana, Geneva, sans-serif;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:linear-gradient(135deg,#101322 0%%,#1b2040 100%%); padding:28px 12px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;">
                                    <tr>
                                        <td align="center" style="padding:0 0 16px 0;">
                                            <h1 style="margin:0; font-size:32px; letter-spacing:1px; color:#ffffff;">MEDIA ODYSSEY</h1>
                                            <p style="margin:8px 0 0 0; color:#d6defd; font-size:15px;">Discover your next obsession!</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background:#171b33; border:1px solid #2a325f; border-radius:16px; padding:28px 24px;">
                                            <h2 style="margin:0 0 12px 0; color:#ffffff; font-size:24px;">Verify your email address</h2>
                                            <p style="margin:0 0 18px 0; color:#d6defd; font-size:15px; line-height:1.6;">
                                                Welcome to %s. Please confirm your email to finish setting up your account.
                                            </p>
                                            <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 8px auto;">
                                                <tr>
                                                    <td align="center" bgcolor="#20c9ff" style="border-radius:10px;">
                                                        <a href="%s" style="display:inline-block; padding:12px 22px; font-weight:700; color:#0b1023; text-decoration:none; font-size:15px;">
                                                            Verify Email
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin:18px 0 0 0; color:#c2caef; font-size:13px; line-height:1.6;">
                                                This link expires in %d minutes.
                                            </p>
                                            <p style="margin:16px 0 0 0; color:#c2caef; font-size:13px; line-height:1.6;">
                                                If the button does not work, copy and paste this link into your browser:
                                            </p>
                                            <p style="margin:8px 0 0 0; word-break:break-all; font-size:13px;">
                                                <a href="%s" style="color:#7ee1ff; text-decoration:underline;">%s</a>
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="padding:16px 8px 0 8px; color:#9fa8d1; font-size:12px; line-height:1.5;">
                                            If you did not create this account, you can safely ignore this email.
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(this.appName, this.appName, verificationUrl, tokenExpiryInMinutes, verificationUrl,
                        verificationUrl);
    }
}
