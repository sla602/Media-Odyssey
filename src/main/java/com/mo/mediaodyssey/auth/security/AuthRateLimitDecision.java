package com.mo.mediaodyssey.auth.security;

public record AuthRateLimitDecision(boolean permitted, long retryAfterSeconds) {

    public static AuthRateLimitDecision allowed() {
        return new AuthRateLimitDecision(true, 0L);
    }

    public static AuthRateLimitDecision denied(long retryAfterSeconds) {
        return new AuthRateLimitDecision(false, Math.max(1L, retryAfterSeconds));
    }
}
