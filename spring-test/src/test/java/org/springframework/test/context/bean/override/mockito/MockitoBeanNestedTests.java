/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * Verifies proper reset of mocks when a {@link MockitoBean @MockitoBean} field
 * is declared in the enclosing class of a {@link Nested @Nested} test class.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 */
@ExtendWith(SpringExtension.class)
// TODO Remove @ContextConfiguration declaration.
// @ContextConfiguration is currently required due to a bug in the TestContext framework.
@ContextConfiguration
class MockitoBeanNestedTests {

	@MockitoBean
	Runnable action;

	@Autowired
	Task task;

	@Test
	void mockWasInvokedOnce() {
		task.execute();
		then(action).should().run();
	}

	@Test
	void mockWasInvokedTwice() {
		task.execute();
		task.execute();
		then(action).should(times(2)).run();
	}

	@Nested
	class MockitoBeanFieldInEnclosingClassTests {

		@Test
		void mockWasInvokedOnce() {
			task.execute();
			then(action).should().run();
		}

		@Test
		void mockWasInvokedTwice() {
			task.execute();
			task.execute();
			then(action).should(times(2)).run();
		}
	}

	record Task(Runnable action) {

		void execute() {
			this.action.run();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		Task task(Runnable action) {
			return new Task(action);
		}
	}

}
