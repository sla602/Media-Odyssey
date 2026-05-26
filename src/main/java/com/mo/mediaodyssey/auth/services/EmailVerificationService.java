package com.mo.mediaodyssey.auth.services;

import java.util.Date;
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

    @Value("${spring.application.name:App}")
    private String appName;

    // Expiry length determined by environment variable.
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
        VerificationToken verificationToken = new VerificationToken(token, user, tokenExpiryInMinutes);

        // Build verification token email
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();
        String verificationUrl = baseUrl + "/auth/verify?token=" + token;
        String to = user.getEmail();
        String subject = "Confirm Registration to " + this.appName;
        String htmlMessage = emailService.buildAuthActionEmailHtml(
                this.appName,
                "Verify Your Email",
                "Verify your email address",
                "Welcome to " + this.appName + ". Please confirm your email to finish setting up your account.",
                "Verify Email",
                verificationUrl,
                tokenExpiryInMinutes,
                "If you did not create this account, you can safely ignore this email.");

        // Send verification token email
        emailService.sendHtmlEmail(to, subject, htmlMessage);

        // Save verification token
        verificationTokenRepository.save(verificationToken);
    }

    @Transactional
    public void verifyUser(VerifyTokenDto dto) {
        VerificationToken tokenEntity = verificationTokenRepository.findByToken(dto.token())
                .filter(item -> item.getExpiryDate().after(new Date()))
                .orElseThrow(() -> new InvalidVerificationTokenException(
                        "Verification token is invalid. Please try again."));
        User user = tokenEntity.getUser();

        // Mark the user's email verification state and save this change.
        user.setEmailVerified(true);
        userRepository.save(user);

        // Delete the verification token because it is no longer needed.
        user.setVerificationToken(null);
        verificationTokenRepository.delete(tokenEntity);
    }

    @Transactional
    public void resendVerification(ResendVerifyTokenDto dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + dto.email()));

        if (!user.isAccountNonLocked()) {
            throw new LockedException("User is locked. Cannot verify locked user. Contact support.");
        }

        if (user.isEmailVerified()) {
            throw new UserAlreadyVerifiedException("User is already verified. Please log in.");
        }

        VerificationToken tokenEntity = user.getVerificationToken();
        if (tokenEntity != null) {
            user.setVerificationToken(null);
            verificationTokenRepository.delete(tokenEntity);
            verificationTokenRepository.flush();
        }

        createVerification(user);
    }
}
