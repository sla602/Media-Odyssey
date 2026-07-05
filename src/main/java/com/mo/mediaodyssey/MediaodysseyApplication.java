package com.mo.mediaodyssey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point for Media Odyssey.
 *
 * Purpose:
 * - Starts the Spring Boot application.
 * - Enables scheduled tasks.
 *
 * Imports and annotations:
 * - {@code SpringApplication}: starts the application context.
 * - {@code @SpringBootApplication}: marks this class as the main Spring Boot
 * configuration and enables auto-configuration and component scanning.
 * - {@code @EnableScheduling}: turns on support for scheduled background tasks.
 *
 * Notes:
 * - Keep this class small; feature logic belongs in other application classes.
 */
@SpringBootApplication
@EnableScheduling
public class MediaodysseyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediaodysseyApplication.class, args);
	}

}