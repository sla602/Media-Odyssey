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
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.mo.mediaodyssey.auth.security.AuthRateLimitScope;
import com.mo.mediaodyssey.auth.security.AuthRateLimitFilter;
import com.mo.mediaodyssey.auth.services.AuthRateLimitService;
import com.mo.mediaodyssey.auth.services.JdbcAuthRateLimitStore;
import com.mo.mediaodyssey.testsupport.MutableClock;

import jakarta.servlet.FilterChain;

@SpringBootTest
class AuthRateLimitFilterTest {

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
    void doFilter_appliesGlobalLimitAcrossAuthEndpoints_butSkipsLogout() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-27T00:00:00Z"), ZoneOffset.UTC);
        AuthRateLimitService service = authRateLimitService(clock, 2, 1, 1);
        AuthRateLimitFilter filter = new AuthRateLimitFilter(service);
        MockHttpSession session = trackedSession();
        String clientIp = trackedClientIp();

        MockHttpServletRequest firstLoginRequest = authRequest("POST", "/api/auth/login", clientIp, session);
        MockHttpServletResponse firstLoginResponse = new MockHttpServletResponse();
        AtomicBoolean firstLoginCalled = new AtomicBoolean(false);

        filter.doFilter(firstLoginRequest, firstLoginResponse, chainThatSets(firstLoginCalled));

        assertTrue(firstLoginCalled.get());
        assertEquals(200, firstLoginResponse.getStatus());

        MockHttpServletRequest secondLoginRequest = authRequest("POST", "/api/auth/login", clientIp, session);
        MockHttpServletResponse secondLoginResponse = new MockHttpServletResponse();
        AtomicBoolean secondLoginCalled = new AtomicBoolean(false);

        filter.doFilter(secondLoginRequest, secondLoginResponse, chainThatSets(secondLoginCalled));

        assertFalse(secondLoginCalled.get());
        assertEquals(429, secondLoginResponse.getStatus());
        assertTrue(secondLoginResponse.getContentType().startsWith("application/json"));
        assertEquals("900", secondLoginResponse.getHeader("Retry-After"));
        assertTrue(secondLoginResponse.getContentAsString().contains("AUTH_RATE_LIMITED"));

        MockHttpServletRequest resetRequest = authRequest("POST", "/api/auth/password/reset", clientIp, session);
        MockHttpServletResponse resetResponse = new MockHttpServletResponse();
        AtomicBoolean resetCalled = new AtomicBoolean(false);

        filter.doFilter(resetRequest, resetResponse, chainThatSets(resetCalled));

        assertFalse(resetCalled.get());
        assertEquals(429, resetResponse.getStatus());
        assertTrue(resetResponse.getContentType().startsWith("application/json"));
        assertEquals("3600", resetResponse.getHeader("Retry-After"));
        assertTrue(resetResponse.getContentAsString().contains("AUTH_RATE_LIMITED"));

        MockHttpServletRequest logoutRequest = authRequest("GET", "/api/auth/logout", clientIp, session);
        MockHttpServletResponse logoutResponse = new MockHttpServletResponse();
        AtomicBoolean logoutCalled = new AtomicBoolean(false);

        filter.doFilter(logoutRequest, logoutResponse, chainThatSets(logoutCalled));

        assertTrue(logoutCalled.get());
        assertEquals(200, logoutResponse.getStatus());
    }

    @Test
    void doFilter_appliesEmailLimitAcrossEmailBasedAuthEndpoints() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-27T00:00:00Z"), ZoneOffset.UTC);
        AuthRateLimitService service = authRateLimitService(clock, 30, 5, 1);
        AuthRateLimitFilter filter = new AuthRateLimitFilter(service);
        MockHttpSession session = trackedSession();
        String clientIp = trackedClientIp();

        MockHttpServletRequest registerRequest = authRequest("POST", "/api/auth/register", clientIp, session);
        MockHttpServletResponse registerResponse = new MockHttpServletResponse();
        AtomicBoolean registerCalled = new AtomicBoolean(false);

        filter.doFilter(registerRequest, registerResponse, chainThatSets(registerCalled));

        assertTrue(registerCalled.get());
        assertEquals(200, registerResponse.getStatus());

        MockHttpServletRequest resendRequest = authRequest("POST", "/api/auth/resend", clientIp, session);
        MockHttpServletResponse resendResponse = new MockHttpServletResponse();
        AtomicBoolean resendCalled = new AtomicBoolean(false);

        filter.doFilter(resendRequest, resendResponse, chainThatSets(resendCalled));

        assertFalse(resendCalled.get());
        assertEquals(429, resendResponse.getStatus());
        assertEquals("1800", resendResponse.getHeader("Retry-After"));
        assertTrue(resendResponse.getContentAsString().contains("AUTH_RATE_LIMITED"));
    }

    private AuthRateLimitService authRateLimitService(MutableClock clock, int globalMaxRequests, int loginMaxRequests,
            int emailMaxRequests) {
        return new AuthRateLimitService(true, globalMaxRequests, Duration.ofMinutes(60), loginMaxRequests,
                Duration.ofMinutes(15), emailMaxRequests, Duration.ofMinutes(30), clock, rateLimitStore);
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

    private FilterChain chainThatSets(AtomicBoolean called) {
        return (request, response) -> called.set(true);
    }
}
