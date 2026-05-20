package com.mo.mediaodyssey;

import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

/**
 * Smoke test that checks whether the Spring application context starts.
 *
 * Purpose:
 * - Load the full Spring Boot context so startup and wiring problems show up
 * during test execution.
 *
 * Imports and annotations:
 * - {@code SpringBootTest}: starts the Spring Boot application context for the
 * test.
 * - {@code Test}: marks {@code contextLoads()} as a JUnit 5 test method.
 *
 * Notes:
 * - This test should stay lightweight and avoid external dependencies.
 */
@SpringBootTest
class MediaodysseyApplicationTests {

	@Test
	void contextLoads() {
	}

}