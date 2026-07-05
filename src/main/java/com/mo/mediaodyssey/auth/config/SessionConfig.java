package com.mo.mediaodyssey.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.PostgreSqlJdbcIndexedSessionRepositoryCustomizer;

/**
 * Configuration class for session management.
 * 
 * This class defines beans for session event publishing and customizes the JDBC
 * session repository for PostgreSQL. It ensures that session events are
 * properly handled and that the session repository is configured to work with
 * PostgreSQL databases.
 */
@Configuration
public class SessionConfig {

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SessionRepositoryCustomizer<JdbcIndexedSessionRepository> postgresJdbcSessionCustomizer() {
        return new PostgreSqlJdbcIndexedSessionRepositoryCustomizer();
    }
}
