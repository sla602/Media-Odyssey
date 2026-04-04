package com.mo.mediaodyssey.auth.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import com.mo.mediaodyssey.auth.security.MOAuthenticationProvider;
import com.mo.mediaodyssey.auth.services.MOOAuth2UserService;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, MOOAuth2UserService customOAuth2UserService,
                        CurrentAccountService currentAccountService,
                        SessionAuthenticationStrategy sessionAuthenticationStrategy)
                        throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement((session) -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                                .sessionAuthenticationStrategy(sessionAuthenticationStrategy))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers( // Public access.
                                                                "/auth/**",
                                                                "/api/auth/**",
                                                                "/api/dev/**",
                                                                "/error",
                                                                "/search",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/images/**")
                                                .permitAll()
                                                .requestMatchers("/admin/**").hasRole("ADMIN") // Admin access only.
                                                .anyRequest().authenticated()) // All others require authentication with
                                                                               // any role.
                                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                                                new LoginUrlAuthenticationEntryPoint("/auth"))) // Redirect
                                                                                                // requests
                                                                                                // requiring
                                                                                                // authentication
                                                                                                // to
                                                                                                // "/auth".
                                .formLogin((form) -> form.disable()) // Disable default Spring Security form login.
                                .httpBasic((basic) -> basic.disable()) // Disable default Spring Security basic HTTP
                                                                       // authentication.
                                .oauth2Login(oauth2 -> oauth2 // Configure Spring Security oauth client with
                                                              // customization.
                                                .loginPage("/auth/login")
                                                .authorizationEndpoint(authEndpoint -> authEndpoint
                                                                .baseUri("/auth/oauth2/authorization"))
                                                .redirectionEndpoint(redirect -> redirect
                                                                .baseUri("/auth/oauth2/callback/*"))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .failureUrl("/auth/login?oauthError=true")
                                                .successHandler((request, response, authentication) -> {
                                                        currentAccountService.refreshPrincipal(authentication, request,
                                                                        response);

                                                        response.sendRedirect("/");
                                                }))
                                .logout((logout) -> logout // Configure Spring Security log out with customization.
                                                .logoutUrl("/api/auth/logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID", "SESSION")
                                                .logoutSuccessHandler((request, response, authentication) -> {
                                                        if ("POST".equalsIgnoreCase(request.getMethod())) {
                                                                new HttpStatusReturningLogoutSuccessHandler(
                                                                                HttpStatus.OK).onLogoutSuccess(request,
                                                                                                response,
                                                                                                authentication);
                                                        } else {
                                                                response.sendRedirect("/auth");
                                                        }
                                                })
                                                .permitAll());
                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SessionRegistry sessionRegistry() {
                return new SessionRegistryImpl();
        }

        @Bean
        public AuthenticationManager authManager(HttpSecurity http, MOAuthenticationProvider moAuthenticationProvider)
                        throws Exception { // Authentication provider for local accounts.
                AuthenticationManagerBuilder authenticationManagerBuilder = http
                                .getSharedObject(AuthenticationManagerBuilder.class);
                authenticationManagerBuilder.authenticationProvider(moAuthenticationProvider);
                return authenticationManagerBuilder.build();
        }

        @Bean
        public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry sessionRegistry) { // Limit
                                                                                                              // maximum
                                                                                                              // concurrent
                                                                                                              // sessions
                                                                                                              // to 1
                                                                                                              // per
                                                                                                              // account.
                                                                                                              // If
                                                                                                              // limit
                                                                                                              // is
                                                                                                              // exceeded,
                                                                                                              // the
                                                                                                              // oldest
                                                                                                              // session
                                                                                                              // is
                                                                                                              // invalidated.
                ConcurrentSessionControlAuthenticationStrategy concurrent = new ConcurrentSessionControlAuthenticationStrategy(
                                sessionRegistry);
                concurrent.setMaximumSessions(1);
                concurrent.setExceptionIfMaximumExceeded(false);

                return new CompositeSessionAuthenticationStrategy(List.of(
                                concurrent,
                                new ChangeSessionIdAuthenticationStrategy(),
                                new RegisterSessionAuthenticationStrategy(sessionRegistry)));
        }
}