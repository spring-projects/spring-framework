/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.async;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Integration tests for applications using {@link Async @Async} methods with
 * {@code @DirtiesContext}.
 *
 * <p>Execute this test class with {@code -Xmx8M} to verify that there are no
 * issues with memory leaks as raised in
 * <a href="https://github.com/spring-projects/spring-framework/issues/23571">gh-23571</a>.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@SpringJUnitConfig
@Disabled("Only meant to be executed manually")
class AsyncMethodsSpringTestContextIntegrationTests {

	@RepeatedTest(200)
	@DirtiesContext
	void test() {
		// If we don't run out of memory, then this test is a success.
	}


	@Configuration
	@EnableAsync
	static class Config {

		@Bean
		AsyncService asyncService() {
			return new AsyncService();
		}
	}

	static class AsyncService {

		@Async
		void asyncMethod() {
		}
	}

}
