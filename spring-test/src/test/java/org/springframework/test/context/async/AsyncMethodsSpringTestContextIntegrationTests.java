/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
