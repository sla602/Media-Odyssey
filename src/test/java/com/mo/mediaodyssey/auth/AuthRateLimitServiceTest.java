package com.mo.mediaodyssey.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import com.mo.mediaodyssey.auth.security.AuthRateLimitDecision;
import com.mo.mediaodyssey.auth.security.AuthRateLimitScope;
import com.mo.mediaodyssey.auth.services.AuthRateLimitService;
import com.mo.mediaodyssey.auth.services.JdbcAuthRateLimitStore;
import com.mo.mediaodyssey.testsupport.MutableClock;

@SpringBootTest
class AuthRateLimitServiceTest {

    private final Set<String> trackedClientKeys = new LinkedHashSet<>();

    @Autowired
    private JdbcAuthRateLimitStore rateLimitStore;

    @AfterEach
    void clearTrackedRateLimitRows() {
        List<String> clientKeys = List.copyOf(trackedClientKeys);
        trackedClientKeys.clear();

        if (clientKeys.isEmpty()) {
            return;
        }

        for (AuthRateLimitScope scope : AuthRateLimitScope.values()) {
            rateLimitStore.clear(scope, clientKeys);
        }
    }

    @Test
    void check_allowsRequestsWithinLimit_thenDeniesWhenLimitIsExceeded() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-27T00:00:00Z"), ZoneOffset.UTC);
        AuthRateLimitService service = authRateLimitService(true, 30, Duration.ofMinutes(60), 2,
                Duration.ofMinutes(1), 2, Duration.ofMinutes(1), clock);
        String clientIp = trackedClientIp();

        assertTrue(service.check(AuthRateLimitScope.LOGIN, clientIp).permitted());
        assertTrue(service.check(AuthRateLimitScope.LOGIN, clientIp).permitted());

        AuthRateLimitDecision decision = service.check(AuthRateLimitScope.LOGIN, clientIp);
        assertFalse(decision.permitted());
        assertEquals(60L, decision.retryAfterSeconds());
    }

    @Test
    void check_resetsAfterTheWindowExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-27T00:00:00Z"), ZoneOffset.UTC);
        AuthRateLimitService service = authRateLimitService(true, 30, Duration.ofMinutes(60), 1,
                Duration.ofMinutes(1), 1, Duration.ofMinutes(1), clock);
        String clientIp = trackedClientIp();

        assertTrue(service.check(AuthRateLimitScope.EMAIL, clientIp).permitted());
        assertFalse(service.check(AuthRateLimitScope.EMAIL, clientIp).permitted());

        clock.advance(Duration.ofMinutes(1).plusSeconds(1));

        assertTrue(service.check(AuthRateLimitScope.EMAIL, clientIp).permitted());
    }

    @Test
    void check_returnsAllowedWhenDisabled() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-27T00:00:00Z"), ZoneOffset.UTC);
        AuthRateLimitService service = authRateLimitService(false, 30, Duration.ofMinutes(60), 1,
                Duration.ofMinutes(1), 1, Duration.ofMinutes(1), clock);
        String clientIp = trackedClientIp();

        assertTrue(service.check(AuthRateLimitScope.LOGIN, clientIp).permitted());
        assertTrue(service.check(AuthRateLimitScope.EMAIL, clientIp).permitted());
    }

    @Test
    void check_globalLimitSharesAcrossSessionAndIpChanges() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-27T00:00:00Z"), ZoneOffset.UTC);
        AuthRateLimitService service = authRateLimitService(true, 1, Duration.ofMinutes(60), 5,
                Duration.ofMinutes(15), 5, Duration.ofMinutes(15), clock);
        MockHttpSession session = trackedSession();

        MockHttpServletRequest firstRequest = authRequest("POST", "/api/auth/login", trackedClientIp(), session);
        assertTrue(service.check(AuthRateLimitScope.GLOBAL, firstRequest).permitted());

        MockHttpServletRequest secondRequest = authRequest("POST", "/api/auth/register", trackedClientIp(), session);
        assertFalse(service.check(AuthRateLimitScope.GLOBAL, secondRequest).permitted());
    }

    private AuthRateLimitService authRateLimitService(boolean enabled, int globalMaxRequests, Duration globalWindow,
            int loginMaxRequests, Duration loginWindow, int emailMaxRequests, Duration emailWindow,
            MutableClock clock) {
        return new AuthRateLimitService(enabled, globalMaxRequests, globalWindow, loginMaxRequests, loginWindow,
                emailMaxRequests, emailWindow, clock, rateLimitStore);
    }

    private String trackedClientIp() {
        String clientIp = "rate-limit-test-" + UUID.randomUUID();
        trackedClientKeys.add("ip:" + clientIp);
        return clientIp;
    }

    private MockHttpSession trackedSession() {
        MockHttpSession session = new MockHttpSession();
        trackedClientKeys.add("session:" + session.getId());
        return session;
    }

    private MockHttpServletRequest authRequest(String method, String uri, String remoteAddr, MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        request.setSession(session);
        return request;
    }
}
