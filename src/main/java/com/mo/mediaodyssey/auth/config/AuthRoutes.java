package com.mo.mediaodyssey.auth.config;

/**
 * Shared auth routes and query parameter names.
 *
 * Keep route strings here so the auth controller, security config, rate
 * limiter, and auth pages all point at the same paths.
 */
public final class AuthRoutes {

    public static final class Api {
        public static final String AUTH_API_PREFIX = "/api/auth";
        public static final String LOGIN = AUTH_API_PREFIX + "/login";
        public static final String REGISTER = AUTH_API_PREFIX + "/register";
        public static final String VERIFY = AUTH_API_PREFIX + "/verify";
        public static final String RESEND = AUTH_API_PREFIX + "/resend";
        public static final String PASSWORD_FORGOT = AUTH_API_PREFIX + "/password/forgot";
        public static final String PASSWORD_RESET = AUTH_API_PREFIX + "/password/reset";
        public static final String LOGOUT = AUTH_API_PREFIX + "/logout";

    }

    public static final class Page {
        public static final String AUTH_PAGE_ROOT = "/auth";
        public static final String LOGIN = AUTH_PAGE_ROOT + "/login";
        public static final String SIGNUP = AUTH_PAGE_ROOT + "/signup";
        public static final String FORGOT = AUTH_PAGE_ROOT + "/forgot";
        public static final String RESET = AUTH_PAGE_ROOT + "/reset";
        public static final String VERIFY = AUTH_PAGE_ROOT + "/verify";
        public static final String RESEND = AUTH_PAGE_ROOT + "/resend";
        public static final String LOGOUT = AUTH_PAGE_ROOT + "/logout";
        public static final String OAUTH2_AUTHORIZATION = AUTH_PAGE_ROOT + "/oauth2/authorization";
        public static final String OAUTH2_CALLBACK = AUTH_PAGE_ROOT + "/oauth2/callback/*";

    }

    public static final class QueryParams {
        public static final String SESSION_EXPIRED = "sessionExpired";
        public static final String OAUTH_ERROR = "oauthError";

    }
}
