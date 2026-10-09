package com.mo.mediaodyssey.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mo.mediaodyssey.auth.model.VerificationToken;
import com.mo.mediaodyssey.auth.dto.VerifyTokenDto;
import com.mo.mediaodyssey.auth.exception.InvalidVerificationTokenException;
import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.auth.repository.VerificationTokenRepository;
import com.mo.mediaodyssey.auth.services.EmailVerificationService;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.EmailService;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final int CONFIGURED_EXPIRY_MINUTES = 37;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailVerificationService, "appName", "Media Odyssey");
        ReflectionTestUtils.setField(emailVerificationService, "tokenExpiryInMinutes", CONFIGURED_EXPIRY_MINUTES);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("mediaodyssey.example");
        request.setServerPort(443);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createVerification_sendsStyledHtmlEmailWithTokenizedVerificationLink() {
        User user = new User("user@mediaodyssey.example", "encoded-password");

        when(emailService.buildAuthActionEmailHtml(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString()))
                .thenAnswer(invocation -> {
                    String actionUrl = invocation.getArgument(5, String.class);
                    Integer expiryMinutes = invocation.getArgument(6, Integer.class);
                    return "<h1>MEDIA ODYSSEY</h1>"
                            + "<a href=\"" + actionUrl + "\">Verify Email</a>"
                            + "<p>This link expires in " + expiryMinutes + " minutes.</p>";
                });

        emailVerificationService.createVerification(user);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);

        verify(emailService).sendHtmlEmail(eq("user@mediaodyssey.example"), subjectCaptor.capture(),
                htmlCaptor.capture());
        verify(verificationTokenRepository).save(tokenCaptor.capture());

        VerificationToken verificationToken = tokenCaptor.getValue();
        String token = verificationToken.getToken();
        String html = htmlCaptor.getValue();

        assertThat(subjectCaptor.getValue()).isEqualTo("Confirm Registration to Media Odyssey");
        assertThat(tokenCaptor.getValue().getUser()).isSameAs(user);
        assertThat(html).contains("MEDIA ODYSSEY");
        assertThat(html).contains("Verify Email");
        assertThat(html).contains("/auth/verify?token=" + token);
        assertThat(html).contains("This link expires in " + CONFIGURED_EXPIRY_MINUTES + " minutes.");

        Instant now = Instant.now();
        Instant expectedMin = now.plus(CONFIGURED_EXPIRY_MINUTES - 1L, ChronoUnit.MINUTES);
        Instant expectedMax = now.plus(CONFIGURED_EXPIRY_MINUTES + 1L, ChronoUnit.MINUTES);
        Instant actualExpiry = verificationToken.getExpiryDate().toInstant();

        assertThat(actualExpiry).isAfter(expectedMin);
        assertThat(actualExpiry).isBefore(expectedMax);
    }

    @Test
    void verifyUser_withValidToken_marksUserVerifiedAndInvalidatesToken() {
        User user = new User("verify-user@mediaodyssey.example", "encoded-password");
        user.setEmailVerified(false);

        VerificationToken tokenEntity = new VerificationToken("valid-token", user, CONFIGURED_EXPIRY_MINUTES);
        tokenEntity.setExpiryDate(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
        user.setVerificationToken(tokenEntity);

        when(verificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(tokenEntity));

        emailVerificationService.verifyUser(new VerifyTokenDto("valid-token"));

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getVerificationToken()).isNull();
        verify(userRepository).save(user);
        verify(verificationTokenRepository).delete(tokenEntity);
    }

    @Test
    void verifyUser_withExpiredToken_throwsValidationError() {
        User user = new User("verify-user@mediaodyssey.example", "encoded-password");
        VerificationToken tokenEntity = new VerificationToken("expired-token", user, CONFIGURED_EXPIRY_MINUTES);
        tokenEntity.setExpiryDate(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)));

        when(verificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(tokenEntity));

        assertThatThrownBy(() -> emailVerificationService.verifyUser(new VerifyTokenDto("expired-token")))
                .isInstanceOf(InvalidVerificationTokenException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(verificationTokenRepository, never()).delete(any(VerificationToken.class));
    }
}
