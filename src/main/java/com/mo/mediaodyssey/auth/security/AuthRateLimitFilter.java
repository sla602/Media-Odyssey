package com.mo.mediaodyssey.auth.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mo.mediaodyssey.auth.config.AuthRoutes;
import com.mo.mediaodyssey.auth.dto.AuthApiResponse;
import com.mo.mediaodyssey.auth.services.AuthRateLimitService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_STATUS = "AUTH_RATE_LIMITED";
    private static final String RATE_LIMIT_MESSAGE = "You have been rate limited. Please try again later.";

    private final AuthRateLimitService authRateLimitService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthRateLimitFilter(AuthRateLimitService authRateLimitService) {
        this.authRateLimitService = authRateLimitService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null) {
            return true;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String contextPath = request.getContextPath();
        String authRoot = contextPath + AuthRoutes.Api.AUTH_API_PREFIX;
        // Logout stays outside the auth rate limiter because it has its own
        // cleanup flow and should remain reachable even when other auth calls are limited.
        return !requestUri.startsWith(authRoot + "/")
                || requestUri.equals(contextPath + AuthRoutes.Api.LOGOUT);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        List<AuthRateLimitDecision> decisions = new ArrayList<>(2);
        AuthRateLimitDecision globalDecision = authRateLimitService.check(AuthRateLimitScope.GLOBAL, request);
        decisions.add(globalDecision);

        AuthRateLimitScope specificScope = resolveSpecificScope(request, request.getRequestURI());
        if (globalDecision.permitted() && specificScope != null) {
            decisions.add(authRateLimitService.check(specificScope, request));
        }

        if (decisions.stream().allMatch(AuthRateLimitDecision::permitted)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        long retryAfterSeconds = decisions.stream()
                .filter(decision -> !decision.permitted())
                .mapToLong(AuthRateLimitDecision::retryAfterSeconds)
                .max()
                .orElse(1L);

        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        String message = buildRateLimitMessage();
        objectMapper.writeValue(response.getWriter(),
                AuthApiResponse.error(RATE_LIMIT_STATUS, message));
    }

    private AuthRateLimitScope resolveSpecificScope(HttpServletRequest request, String requestUri) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        String contextPath = request.getContextPath();

        if (requestUri.equals(contextPath + AuthRoutes.Api.LOGIN)) {
            return AuthRateLimitScope.LOGIN;
        }

        if (requestUri.equals(contextPath + AuthRoutes.Api.REGISTER)
                || requestUri.equals(contextPath + AuthRoutes.Api.RESEND)
                || requestUri.equals(contextPath + AuthRoutes.Api.PASSWORD_FORGOT)) {
            return AuthRateLimitScope.EMAIL;
        }

        return null;
    }

    private String buildRateLimitMessage() {
        return RATE_LIMIT_MESSAGE;
    }
}
