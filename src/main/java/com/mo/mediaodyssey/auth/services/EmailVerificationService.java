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

    @Value("${email.verifytoken.expiry-in-minutes:}")
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
        String to = user.getEmail();
        String subject = "Confirm Registration to " + this.appName;
        String message = "Confirm registration by clicking this link: " + baseUrl + "/api/auth/verify?token=" + token;

        // Send verification token email
        emailService.sendEmail(to, subject, message);

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

            // Enable the user and save this change.
            user.setEnabled(true);
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
            } else if (user.isEnabled()) { // If User is already email verified.
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
}
