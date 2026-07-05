window.AuthRoutes = (() => {
    const AUTH_API_PREFIX = "/api/auth";
    const AUTH_PAGE_ROOT = "/auth";

    return Object.freeze({
        Api: Object.freeze({
            AUTH_API_PREFIX,
            LOGIN: AUTH_API_PREFIX + "/login",
            REGISTER: AUTH_API_PREFIX + "/register",
            VERIFY: AUTH_API_PREFIX + "/verify",
            RESEND: AUTH_API_PREFIX + "/resend",
            PASSWORD_FORGOT: AUTH_API_PREFIX + "/password/forgot",
            PASSWORD_RESET: AUTH_API_PREFIX + "/password/reset",
            LOGOUT: AUTH_API_PREFIX + "/logout",
        }),
        Page: Object.freeze({
            AUTH_PAGE_ROOT,
            LOGIN: AUTH_PAGE_ROOT + "/login",
            SIGNUP: AUTH_PAGE_ROOT + "/signup",
            FORGOT: AUTH_PAGE_ROOT + "/forgot",
            RESET: AUTH_PAGE_ROOT + "/reset",
            VERIFY: AUTH_PAGE_ROOT + "/verify",
            RESEND: AUTH_PAGE_ROOT + "/resend",
            LOGOUT: AUTH_PAGE_ROOT + "/logout",
            OAUTH2_AUTHORIZATION: AUTH_PAGE_ROOT + "/oauth2/authorization",
            OAUTH2_CALLBACK: AUTH_PAGE_ROOT + "/oauth2/callback/*",
        }),
        QueryParams: Object.freeze({
            SESSION_EXPIRED: "sessionExpired",
            OAUTH_ERROR: "oauthError",
        }),
    });
})();
