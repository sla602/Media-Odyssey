package com.mo.mediaodyssey.auth.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;

import com.mo.mediaodyssey.auth.security.AuthRateLimitFilter;
import com.mo.mediaodyssey.auth.security.MOAuthenticationProvider;
import com.mo.mediaodyssey.auth.services.AuthRateLimitService;
import com.mo.mediaodyssey.auth.services.MOOidcUserService;
import com.mo.mediaodyssey.auth.services.MOUserDetailsService;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;

/**
 * Configuration class for setting up Spring Security.
 * 
 * This class defines the security filter chain, authentication manager, session
 * management, remember me services, and other security-related beans for the
 * application. It configures authentication, authorization, session handling,
 * and rate limiting for the authentication endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private static final int MAX_CONCURRENT_SESSIONS = 1;
        private final String devRole;

        // Constructor to initialize the SecurityConfig with the dev mode role from
        // application properties. If dev mode is enabled but dev mode role is not
        // specified, access will not be restricted for dev endpoints.
        public SecurityConfig(@Value("${dev.mode.role}") String devRole) {
                if (devRole != null) {
                        String normalizedRole = devRole.strip().toUpperCase();
                        this.devRole = normalizedRole.isEmpty() ? "PUBLIC" : normalizedRole;
                } else {
                        this.devRole = "PUBLIC";
                }
        }

        // Configure Spring Security filter chain with customization.
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, MOOidcUserService customOidcUserService,
                        CurrentAccountService currentAccountService,
                        AuthRateLimitService authRateLimitService,
                        SessionAuthenticationStrategy sessionAuthenticationStrategy,
                        SessionRegistry sessionRegistry,
                        RememberMeServices rememberMeServices) {
                // Disable default Spring Security CSRF protection.
                http.csrf((csrf) -> csrf.disable());
                // Disable default Spring Security form login.
                http.formLogin((form) -> form.disable());
                // Disable default Spring Security basic HTTP authentication.
                http.httpBasic((basic) -> basic.disable());
                // Configure session management.
                http.sessionManagement((session) -> {
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
                        session.sessionAuthenticationStrategy(sessionAuthenticationStrategy);
                        session.sessionConcurrency((concurrency) -> {
                                concurrency.maximumSessions(MAX_CONCURRENT_SESSIONS);
                                concurrency.maxSessionsPreventsLogin(false);
                                concurrency.sessionRegistry(sessionRegistry);
                                concurrency.expiredUrl(AuthRoutes.Page.LOGIN + "?"
                                                + AuthRoutes.QueryParams.SESSION_EXPIRED + "=true");
                        });
                });
                // Configure authorization rules.
                http.authorizeHttpRequests(auth -> {
                        // Admin endpoints permit admin access only.
                        auth.requestMatchers("/admin/**").hasRole("ADMIN");
                        // Dev endpoints permit access based on customizable role set as the dev role.
                        if (this.devRole.equals("PUBLIC")) {
                                auth.requestMatchers("/api/dev/**").permitAll();
                        } else {
                                auth.requestMatchers("/api/dev/**").hasRole(this.devRole);
                        }
                        // Allow public access to auth and misc resources.
                        auth.requestMatchers(
                                        AuthRoutes.Page.AUTH_PAGE_ROOT + "/**",
                                        AuthRoutes.Api.AUTH_API_PREFIX + "/**",
                                        "/error",
                                        "/search",
                                        "/css/**",
                                        "/js/**",
                                        "/images/**",
                                        "/favicon.ico",
                                        "/apple-touch-icon.png",
                                        "/apple-touch-icon-precomposed.png")
                                        .permitAll();
                        // All others require authentication with any role.
                        auth.anyRequest().authenticated();
                });
                // Redirect requests requiring authentication to the auth landing page.
                http.exceptionHandling(ex -> ex.authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint(AuthRoutes.Page.AUTH_PAGE_ROOT)));
                // Rate limit the local auth endpoints at the filter layer.
                // Tomcat resolves the trusted proxy IP header configured in
                // application.properties.
                http.addFilterBefore(new AuthRateLimitFilter(authRateLimitService), LogoutFilter.class);
                // Configure Spring Security Remember Me with customization.
                http.rememberMe((remember) -> remember.rememberMeServices(rememberMeServices));
                // Configure Spring Security OIDC client with customization.
                http.oauth2Login(oauth2 -> {
                        oauth2.loginPage(AuthRoutes.Page.LOGIN);
                        oauth2.authorizationEndpoint(
                                        authEndpoint -> authEndpoint.baseUri(AuthRoutes.Page.OAUTH2_AUTHORIZATION));
                        oauth2.redirectionEndpoint(redirect -> redirect.baseUri(AuthRoutes.Page.OAUTH2_CALLBACK));
                        oauth2.userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService));
                        oauth2.failureUrl(AuthRoutes.Page.LOGIN + "?" + AuthRoutes.QueryParams.OAUTH_ERROR + "=true");
                        oauth2.successHandler((request, response, authentication) -> {
                                currentAccountService.refreshPrincipal(authentication, request, response);
                                response.sendRedirect("/");
                        });
                });
                // Configure Spring Security log out with customization.
                http.logout((logout) -> {
                        logout.logoutUrl(AuthRoutes.Api.LOGOUT);
                        logout.invalidateHttpSession(true);
                        logout.clearAuthentication(true);
                        logout.deleteCookies("JSESSIONID", "SESSION", "remember-me");
                        logout.logoutSuccessHandler((request, response, authentication) -> {
                                if ("POST".equalsIgnoreCase(request.getMethod())) {
                                        new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK)
                                                        .onLogoutSuccess(request, response, authentication);
                                } else {
                                        response.sendRedirect(AuthRoutes.Page.AUTH_PAGE_ROOT);
                                }
                        }).permitAll();
                });

                return http.build();
        }

        // Configure Spring Security Remember Me with customization.
        @Bean
        public TokenBasedRememberMeServices rememberMeServices(MOUserDetailsService userDetailsService,
                        @Value("${security.remember-me.key}") String rememberMeKey) {
                TokenBasedRememberMeServices rememberMeServices = new TokenBasedRememberMeServices(
                                rememberMeKey,
                                userDetailsService::loadUserByUsername);
                rememberMeServices.setTokenValiditySeconds(14 * 24 * 60 * 60); // 14 days remember-me token validity
                rememberMeServices.setParameter("remember-me");
                // Keep remember-me disabled by default. AuthController invokes token creation
                // only when the User explicitly checks the remember-me option.
                rememberMeServices.setAlwaysRemember(false);
                return rememberMeServices;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        // Enforce 1 concurrent session per account across all instances of the
        // application.
        @Bean
        public <S extends Session> SessionRegistry sessionRegistry(
                        FindByIndexNameSessionRepository<S> sessionRepository) {
                return new SpringSessionBackedSessionRegistry<>(sessionRepository);
        }

        // Authentication provider for local accounts.
        @Bean
        public AuthenticationManager authManager(HttpSecurity http, MOAuthenticationProvider moAuthenticationProvider) {
                AuthenticationManagerBuilder authenticationManagerBuilder = http
                                .getSharedObject(AuthenticationManagerBuilder.class);
                authenticationManagerBuilder.authenticationProvider(moAuthenticationProvider);
                return authenticationManagerBuilder.build();
        }

        // Limit maximum concurrent sessions to 1 per account.
        // If limit is exceeded, the oldest session is invalidated.
        @Bean
        public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry sessionRegistry) {
                ConcurrentSessionControlAuthenticationStrategy concurrent = new ConcurrentSessionControlAuthenticationStrategy(
                                sessionRegistry);
                concurrent.setMaximumSessions(MAX_CONCURRENT_SESSIONS);
                concurrent.setExceptionIfMaximumExceeded(false);

                return new CompositeSessionAuthenticationStrategy(List.of(
                                concurrent,
                                new ChangeSessionIdAuthenticationStrategy(),
                                new RegisterSessionAuthenticationStrategy(sessionRegistry)));
        }
}
