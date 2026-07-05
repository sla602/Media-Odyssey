package com.mo.mediaodyssey.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
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
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mo.mediaodyssey.auth.dto.ForgotPasswordDto;
import com.mo.mediaodyssey.auth.dto.ResetPasswordDto;
import com.mo.mediaodyssey.auth.exception.InvalidPasswordResetTokenException;
import com.mo.mediaodyssey.auth.exception.PasswordResetNotAllowedException;
import com.mo.mediaodyssey.auth.model.PasswordResetToken;
import com.mo.mediaodyssey.auth.repository.PasswordResetTokenRepository;
import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.auth.services.AuthRateLimitService;
import com.mo.mediaodyssey.auth.services.PasswordResetService;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.EmailService;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final int CONFIGURED_EXPIRY_MINUTES = 29;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private SessionRegistry sessionRegistry;

    @Mock
    private AuthRateLimitService authRateLimitService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "appName", "Media Odyssey");
        ReflectionTestUtils.setField(passwordResetService, "tokenExpiryInMinutes", CONFIGURED_EXPIRY_MINUTES);

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
    void requestPasswordReset_forLocalUser_sendsEmailAndStoresToken() {
        User user = new User("user@mediaodyssey.example", "encoded-password");
        user.setAuthProvider("LOCAL");
        when(userRepository.findByEmail("user@mediaodyssey.example")).thenReturn(Optional.of(user));
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
                    String actionLabel = invocation.getArgument(4, String.class);
                    String actionUrl = invocation.getArgument(5, String.class);
                    Integer expiryMinutes = invocation.getArgument(6, Integer.class);
                    return "<h1>MEDIA ODYSSEY</h1>"
                            + "<a href=\"" + actionUrl + "\">" + actionLabel + "</a>"
                            + "<p>This link expires in " + expiryMinutes + " minutes.</p>";
                });

        passwordResetService.requestPasswordReset(new ForgotPasswordDto("user@mediaodyssey.example"));

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        verify(emailService).sendHtmlEmail(eq("user@mediaodyssey.example"), subjectCaptor.capture(),
                htmlCaptor.capture());
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken tokenEntity = tokenCaptor.getValue();
        String token = tokenEntity.getToken();
        String html = htmlCaptor.getValue();

        assertThat(subjectCaptor.getValue()).isEqualTo("Reset Password for Media Odyssey");
        assertThat(tokenEntity.getUser()).isSameAs(user);
        assertThat(html).contains("MEDIA ODYSSEY");
        assertThat(html).contains("Reset Password");
        assertThat(html).contains("/auth/reset?token=" + token);
        assertThat(html).contains("This link expires in " + CONFIGURED_EXPIRY_MINUTES + " minutes.");

        Instant now = Instant.now();
        Instant expectedMin = now.plus(CONFIGURED_EXPIRY_MINUTES - 1L, ChronoUnit.MINUTES);
        Instant expectedMax = now.plus(CONFIGURED_EXPIRY_MINUTES + 1L, ChronoUnit.MINUTES);
        Instant actualExpiry = tokenEntity.getExpiryDate().toInstant();

        assertThat(actualExpiry).isAfter(expectedMin);
        assertThat(actualExpiry).isBefore(expectedMax);
    }

    @Test
    void requestPasswordReset_forOauthUser_blocksResetWithClearError() {
        User oauthUser = new User("oauth-user@mediaodyssey.example", "unused-password");
        oauthUser.setAuthProvider("OAUTH");
        when(userRepository.findByEmail("oauth-user@mediaodyssey.example")).thenReturn(Optional.of(oauthUser));

        assertThatThrownBy(
                () -> passwordResetService
                        .requestPasswordReset(new ForgotPasswordDto("oauth-user@mediaodyssey.example")))
                .isInstanceOf(PasswordResetNotAllowedException.class)
                .hasMessageContaining("provider-based sign-in");

        verifyNoInteractions(emailService);
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void requestPasswordReset_forLockedUser_blocksReset() {
        User lockedUser = new User("locked-user@mediaodyssey.example", "encoded-password");
        lockedUser.setAuthProvider("LOCAL");
        lockedUser.setAccountNonLocked(false);
        when(userRepository.findByEmail("locked-user@mediaodyssey.example")).thenReturn(Optional.of(lockedUser));

        assertThatThrownBy(
                () -> passwordResetService
                        .requestPasswordReset(new ForgotPasswordDto("locked-user@mediaodyssey.example")))
                .isInstanceOf(LockedException.class);

        verifyNoInteractions(emailService, passwordResetTokenRepository);
    }

    @Test
    void requestPasswordReset_withUnknownEmail_completesSilently() {
        when(userRepository.findByEmail("missing@mediaodyssey.example")).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset(new ForgotPasswordDto("missing@mediaodyssey.example"));

        verifyNoInteractions(emailService, passwordResetTokenRepository);
    }

    @Test
    void resetPassword_withValidToken_updatesPasswordInvalidatesTokenAndExpiresSessions() {
        User user = new User("local-user@mediaodyssey.example", "old-encoded-password");
        user.setAuthProvider("LOCAL");
        user.setId(42L);

        PasswordResetToken tokenEntity = new PasswordResetToken("valid-reset-token", user, CONFIGURED_EXPIRY_MINUTES);
        tokenEntity.setExpiryDate(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)));
        user.setPasswordResetToken(tokenEntity);

        when(passwordResetTokenRepository.findByToken("valid-reset-token")).thenReturn(Optional.of(tokenEntity));
        when(passwordEncoder.encode("new-plain-password")).thenReturn("new-encoded-password");

        SessionInformation activeSession = mock(SessionInformation.class);
        when(sessionRegistry.getAllSessions("local-user@mediaodyssey.example", false))
                .thenReturn(List.of(activeSession));

        passwordResetService.resetPassword(new ResetPasswordDto("valid-reset-token", "new-plain-password"));

        assertThat(user.getPassword()).isEqualTo("new-encoded-password");
        assertThat(user.getPasswordResetToken()).isNull();

        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).delete(tokenEntity);
        verify(authRateLimitService).refundSuccessfulPasswordReset();
        verify(sessionRegistry).getAllSessions("local-user@mediaodyssey.example", false);
        verify(activeSession).expireNow();
    }

    @Test
    void resetPassword_withInvalidOrExpiredToken_throwsValidationError() {
        when(passwordResetTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> passwordResetService.resetPassword(new ResetPasswordDto("missing-token", "new-password")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        verifyNoInteractions(passwordEncoder, userRepository, sessionRegistry, authRateLimitService);
    }

    @Test
    void resetPassword_withLockedUser_blocksResetAndSkipsMutation() {
        User lockedUser = new User("locked-user@mediaodyssey.example", "old-password");
        lockedUser.setAuthProvider("LOCAL");
        lockedUser.setAccountNonLocked(false);

        PasswordResetToken tokenEntity = new PasswordResetToken("locked-token", lockedUser, CONFIGURED_EXPIRY_MINUTES);
        tokenEntity.setExpiryDate(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)));

        when(passwordResetTokenRepository.findByToken("locked-token")).thenReturn(Optional.of(tokenEntity));

        assertThatThrownBy(
                () -> passwordResetService.resetPassword(new ResetPasswordDto("locked-token", "new-password")))
                .isInstanceOf(LockedException.class);

        verifyNoInteractions(passwordEncoder, userRepository, sessionRegistry, authRateLimitService);
        verify(passwordResetTokenRepository, never()).delete(any());
    }

    @Test
    void resetPassword_withOauthAccount_throwsNotAllowedAndSkipsMutation() {
        User oauthUser = new User("oauth-user@mediaodyssey.example", "old-password");
        oauthUser.setAuthProvider("OAUTH");

        PasswordResetToken tokenEntity = new PasswordResetToken("oauth-token", oauthUser, CONFIGURED_EXPIRY_MINUTES);
        tokenEntity.setExpiryDate(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)));

        when(passwordResetTokenRepository.findByToken("oauth-token")).thenReturn(Optional.of(tokenEntity));

        assertThatThrownBy(
                () -> passwordResetService.resetPassword(new ResetPasswordDto("oauth-token", "new-password")))
                .isInstanceOf(PasswordResetNotAllowedException.class)
                .hasMessageContaining("provider-based sign-in");

        verifyNoInteractions(passwordEncoder, userRepository, sessionRegistry, authRateLimitService);
        verify(passwordResetTokenRepository, never()).delete(any());
    }
}
