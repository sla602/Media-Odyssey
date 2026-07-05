package com.mo.mediaodyssey.auth.services;

import java.util.List;

import com.mo.mediaodyssey.auth.security.AuthRateLimitDecision;
import com.mo.mediaodyssey.auth.security.AuthRateLimitScope;

interface AuthRateLimitStore {

    AuthRateLimitDecision checkAndRecord(AuthRateLimitScope scope, List<String> clientKeys, int maxRequests,
            long windowMillis, long nowMillis);

    void clear(AuthRateLimitScope scope, List<String> clientKeys);

    void removeMostRecent(AuthRateLimitScope scope, List<String> clientKeys, int requestCount);

    void cleanup(long expiredBeforeMillis, long staleBucketBeforeMillis, long nowMillis);
}
