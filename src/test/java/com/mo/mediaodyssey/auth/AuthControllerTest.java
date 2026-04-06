package com.mo.mediaodyssey.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mo.mediaodyssey.auth.controller.AuthController;
import com.mo.mediaodyssey.auth.controller.AuthExceptionHandler;
import com.mo.mediaodyssey.auth.dto.LoginDto;
import com.mo.mediaodyssey.auth.dto.ResendVerifyTokenDto;
import com.mo.mediaodyssey.auth.dto.UserDto;
import com.mo.mediaodyssey.auth.exception.InvalidVerificationTokenException;
import com.mo.mediaodyssey.auth.services.EmailVerificationService;
import com.mo.mediaodyssey.auth.services.MOLocalAuthService;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MOLocalAuthService authService;

    @MockitoBean
    private EmailVerificationService verificationService;

    @MockitoBean
    private SessionAuthenticationStrategy sessionAuthenticationStrategy;

    @MockitoBean
    private CurrentAccountService currentAccountService;

    @MockitoBean
    private TokenBasedRememberMeServices rememberMeServices;

    @AfterEach
    void cleanupAuthState() {
        // Keep test credentials, auth tokens, and stubs scoped to each test case.
        SecurityContextHolder.clearContext();
        reset(authService, verificationService, sessionAuthenticationStrategy, currentAccountService,
                rememberMeServices);
    }

    @Test
    void login_withValidCredentials_returnsSuccess() throws Exception {
        String email = randomEmail("login-user");
        String password = randomSecret("login-password");
        Authentication authentication = new UsernamePasswordAuthenticationToken(email, password);
        when(authService.loginUser(any(LoginDto.class))).thenReturn(authentication);

        String payload = loginPayload(email, password, false);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("AUTH_LOGIN_SUCCESS"));

        verify(sessionAuthenticationStrategy).onAuthentication(any(Authentication.class), any(), any());
        verify(currentAccountService).refreshPrincipal(any(Authentication.class), any(), any());
        verify(rememberMeServices).loginFail(any(), any());
        verify(rememberMeServices, never()).onLoginSuccess(any(), any(), any(Authentication.class));
    }

    @Test
    void login_withRememberMeEnabled_callsRememberMeLoginSuccess() throws Exception {
        String email = randomEmail("remember-me-user");
        String password = randomSecret("remember-me-password");
        Authentication authentication = new UsernamePasswordAuthenticationToken(email, password);
        when(authService.loginUser(any(LoginDto.class))).thenReturn(authentication);

        String payload = loginPayload(email, password, true);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("AUTH_LOGIN_SUCCESS"));

        verify(rememberMeServices).onLoginSuccess(any(), any(), any(Authentication.class));
        verify(rememberMeServices, never()).loginFail(any(), any());
    }

    @Test
    void login_withInvalidCredentials_returnsUnauthorized() throws Exception {
        when(authService.loginUser(any(LoginDto.class))).thenThrow(new BadCredentialsException("bad credentials"));

        String payload = loginPayload(randomEmail("invalid-login"), randomSecret("wrong-password"), false);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void login_withMissingFields_returnsBadRequest() throws Exception {
        String payload = loginPayload("", "", false);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("AUTH_BAD_REQUEST"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_withValidData_returnsSuccess() throws Exception {
        doNothing().when(authService).registerUser(any(UserDto.class));

        String payload = registerPayload(randomEmail("register-user"), randomSecret("register-password"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("AUTH_REGISTER_SUCCESS"));

        verify(authService).registerUser(any(UserDto.class));
    }

    @Test
    void register_withMissingFields_returnsBadRequest() throws Exception {
        String payload = objectMapper.writeValueAsString(new UserDto("", ""));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("AUTH_BAD_REQUEST"));

        verifyNoInteractions(authService);
    }

    @Test
    void verify_withValidToken_redirectsToAuthLogin() throws Exception {
        doNothing().when(verificationService).verifyUser(any());

        mockMvc.perform(get("/api/auth/verify").param("token", randomSecret("verify-token")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/auth/login"));

        verify(verificationService).verifyUser(any());
    }

    @Test
    void verify_withInvalidToken_returnsBadRequest() throws Exception {
        doThrow(new InvalidVerificationTokenException("invalid token"))
                .when(verificationService)
                .verifyUser(any());

        mockMvc.perform(get("/api/auth/verify").param("token", randomSecret("invalid-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("AUTH_INVALID_VERIFICATION_TOKEN"));
    }

    @Test
    void resend_withValidEmail_returnsSuccess() throws Exception {
        doNothing().when(verificationService).resendVerification(any(ResendVerifyTokenDto.class));

        String payload = resendPayload(randomEmail("verified-user"));

        mockMvc.perform(post("/api/auth/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("AUTH_RESEND_SUCCESS"));

        verify(verificationService).resendVerification(any(ResendVerifyTokenDto.class));
    }

    @Test
    void resend_withMissingEmail_returnsBadRequest() throws Exception {
        String payload = resendPayload("");

        mockMvc.perform(post("/api/auth/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("AUTH_BAD_REQUEST"));

        verifyNoInteractions(verificationService);
    }

    @Test
    void resend_withNonexistentEmail_returnsUnauthorized() throws Exception {
        doThrow(new UsernameNotFoundException("missing"))
                .when(verificationService)
                .resendVerification(any(ResendVerifyTokenDto.class));

        String payload = resendPayload(randomEmail("missing-user"));

        mockMvc.perform(post("/api/auth/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("AUTH_USER_NOT_FOUND"));
    }

    private String loginPayload(String email, String password, boolean rememberMe) throws Exception {
        return objectMapper.writeValueAsString(new LoginDto(email, password, rememberMe));
    }

    private String registerPayload(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new UserDto(email, password));
    }

    private String resendPayload(String email) throws Exception {
        return objectMapper.writeValueAsString(new ResendVerifyTokenDto(email));
    }

    private String randomEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID() + "@example.invalid";
    }

    private String randomSecret(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
