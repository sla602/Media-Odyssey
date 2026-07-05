package com.mo.mediaodyssey.auth.services;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mo.mediaodyssey.auth.security.AuthRateLimitDecision;
import com.mo.mediaodyssey.auth.security.AuthRateLimitScope;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class AuthRateLimitService {

    private static final String UNKNOWN_CLIENT_IP = "unknown";
    private static final String CLIENT_IP_PREFIX = "ip:";
    private static final String SESSION_ID_PREFIX = "session:";
    private static final long CLEANUP_STALE_MULTIPLIER = 2L;

    private final boolean enabled;
    private final RateLimitConfig globalConfig;
    private final RateLimitConfig loginConfig;
    private final RateLimitConfig emailConfig;
    private final Clock clock;
    private final AuthRateLimitStore rateLimitStore;

    @Autowired
    public AuthRateLimitService(@Value("${auth.rate-limit.enabled}") boolean enabled,
            @Value("${auth.rate-limit.global.max-requests}") int globalMaxRequests,
            @Value("${auth.rate-limit.global.window}") Duration globalWindow,
            @Value("${auth.rate-limit.login.max-requests}") int loginMaxRequests,
            @Value("${auth.rate-limit.login.window}") Duration loginWindow,
            @Value("${auth.rate-limit.email.max-requests}") int emailMaxRequests,
            @Value("${auth.rate-limit.email.window}") Duration emailWindow,
            AuthRateLimitStore rateLimitStore) {
        this(enabled, globalMaxRequests, globalWindow, loginMaxRequests, loginWindow, emailMaxRequests, emailWindow,
                Clock.systemUTC(), rateLimitStore);
    }

    public AuthRateLimitService(boolean enabled, int globalMaxRequests, Duration globalWindow, int loginMaxRequests,
            Duration loginWindow, int emailMaxRequests, Duration emailWindow, Clock clock,
            JdbcAuthRateLimitStore rateLimitStore) {
        this(enabled, globalMaxRequests, globalWindow, loginMaxRequests, loginWindow, emailMaxRequests, emailWindow,
                clock, (AuthRateLimitStore) rateLimitStore);
    }

    AuthRateLimitService(boolean enabled, int globalMaxRequests, Duration globalWindow, int loginMaxRequests,
            Duration loginWindow, int emailMaxRequests, Duration emailWindow, Clock clock,
            AuthRateLimitStore rateLimitStore) {
        this.enabled = enabled;
        this.globalConfig = new RateLimitConfig(globalMaxRequests, globalWindow, "global");
        this.loginConfig = new RateLimitConfig(loginMaxRequests, loginWindow, "login");
        this.emailConfig = new RateLimitConfig(emailMaxRequests, emailWindow, "email");
        this.clock = clock;
        this.rateLimitStore = Objects.requireNonNull(rateLimitStore, "rateLimitStore");

        validateConfig(this.globalConfig);
        validateConfig(this.loginConfig);
        validateConfig(this.emailConfig);
    }

    /**
     * Checks a request against the selected rate-limit scope.
     *
     * The request is counted by trusted client IP and, when a session already
     * exists, by session id too. Tomcat normalizes the trusted proxy IP header
     * before this method reads {@code request.getRemoteAddr()}.
     */
    public AuthRateLimitDecision check(AuthRateLimitScope scope, HttpServletRequest request) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(request, "request");

        if (!enabled) {
            return AuthRateLimitDecision.allowed();
        }

        return check(scope, resolveClientKeys(request));
    }

    /**
     * Test-friendly entry point for a single client IP key.
     */
    public AuthRateLimitDecision check(AuthRateLimitScope scope, String clientIp) {
        Objects.requireNonNull(scope, "scope");

        if (!enabled) {
            return AuthRateLimitDecision.allowed();
        }

        return check(scope, List.of(buildIpKey(clientIp)));
    }

    /**
     * Restores login headroom after a valid password reset.
     *
     * This lets a user who just changed their password try the new password right
     * away, even if failed attempts previously exhausted the login bucket.
     */
    public void refundSuccessfulPasswordReset() {
        if (!enabled) {
            return;
        }

        HttpServletRequest request = currentRequest();
        if (request == null) {
            return;
        }

        List<String> clientKeys = resolveClientKeys(request);
        rateLimitStore.clear(AuthRateLimitScope.LOGIN, clientKeys);
        rateLimitStore.removeMostRecent(AuthRateLimitScope.GLOBAL, clientKeys, loginConfig.maxRequests());
    }

    /**
     * Removes old request rows so rate-limit storage stays small.
     */
    @Scheduled(fixedDelay = 300_000)
    void cleanupStaleEntries() {
        if (!enabled) {
            return;
        }

        long nowMillis = clock.millis();
        long maxWindowMillis = maxWindowMillis();
        rateLimitStore.cleanup(nowMillis - maxWindowMillis,
                nowMillis - (maxWindowMillis * CLEANUP_STALE_MULTIPLIER), nowMillis);
    }

    private AuthRateLimitDecision check(AuthRateLimitScope scope, List<String> clientKeys) {
        RateLimitConfig config = configFor(scope);
        return rateLimitStore.checkAndRecord(scope, clientKeys, config.maxRequests(), config.windowMillis(),
                clock.millis());
    }

    private RateLimitConfig configFor(AuthRateLimitScope scope) {
        return switch (scope) {
            case GLOBAL -> globalConfig;
            case LOGIN -> loginConfig;
            case EMAIL -> emailConfig;
        };
    }

    private List<String> resolveClientKeys(HttpServletRequest request) {
        LinkedHashSet<String> clientKeys = new LinkedHashSet<>(2);
        clientKeys.add(buildIpKey(request.getRemoteAddr()));

        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionId = session.getId();
            if (sessionId != null && !sessionId.isBlank()) {
                clientKeys.add(buildSessionKey(sessionId));
            }
        }

        return List.copyOf(clientKeys);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }

        return servletRequestAttributes.getRequest();
    }

    private String buildIpKey(String clientIp) {
        return CLIENT_IP_PREFIX + normalizeClientIp(clientIp);
    }

    private String buildSessionKey(String sessionId) {
        return SESSION_ID_PREFIX + sessionId;
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return UNKNOWN_CLIENT_IP;
        }

        return clientIp.strip();
    }

    private void validateConfig(RateLimitConfig config) {
        if (config.maxRequests() < 1) {
            throw new IllegalArgumentException(
                    "Auth rate limit for " + config.label() + " must allow at least one request.");
        }
        if (config.window() == null || config.window().isZero() || config.window().isNegative()
                || config.windowMillis() < 1L) {
            throw new IllegalArgumentException(
                    "Auth rate limit window for " + config.label() + " must be a positive duration.");
        }
    }

    private long maxWindowMillis() {
        return Math.max(globalConfig.windowMillis(), Math.max(loginConfig.windowMillis(), emailConfig.windowMillis()));
    }

    private record RateLimitConfig(int maxRequests, Duration window, String label) {
        private long windowMillis() {
            return window.toMillis();
        }
    }
}
