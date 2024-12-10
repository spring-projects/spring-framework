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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.context.bean.override.mockito.MockReset.AFTER;
import static org.springframework.test.context.bean.override.mockito.MockReset.BEFORE;
import static org.springframework.test.mockito.MockitoAssertions.assertIsMock;

/**
 * Integration tests for {@link MockitoBean @MockitoBean} where duplicate mocks
 * are created to replace the same existing bean, selected by-type.
 *
 * <p>In other words, this test class demonstrates how one {@code @MockitoBean}
 * can silently override another {@code @MockitoBean}.
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34056">gh-34056</a>
 * @see MockitoBeanDuplicateTypeCreationIntegrationTests
 * @see MockitoSpyBeanDuplicateTypeIntegrationTests
 */
@SpringJUnitConfig
public class MockitoBeanDuplicateTypeReplacementIntegrationTests {

	@MockitoBean(reset = AFTER)
	ExampleService mock1;

	@MockitoBean(reset = BEFORE)
	ExampleService mock2;

	@Autowired
	List<ExampleService> services;

	/**
	 * One could argue that we would ideally expect an exception to be thrown when
	 * two competing mocks are created to replace the same existing bean; however,
	 * we currently only log a warning in such cases.
	 * <p>This method therefore asserts the status quo in terms of behavior.
	 * <p>And the log can be manually checked to verify that an appropriate
	 * warning was logged.
	 */
	@Test
	void onlyOneMockShouldHaveBeenCreated() {
		// Currently logs something similar to the following.
		//
		// WARN - Bean with name 'exampleService' was overridden by multiple handlers:
		// [MockitoBeanOverrideHandler@5478ce1e ..., MockitoBeanOverrideHandler@5edc70ed ...]

		// Last one wins: there's only one physical mock
		assertThat(services).containsExactly(mock2);
		assertThat(mock1).isSameAs(mock2);

		assertIsMock(mock2);
		assertThat(MockReset.get(mock2)).as("MockReset").isEqualTo(BEFORE);

		assertThat(mock2.greeting()).isNull();
		given(mock2.greeting()).willReturn("mocked");
		assertThat(mock2.greeting()).isEqualTo("mocked");
	}


	@Configuration
	static class Config {

		@Bean
		ExampleService exampleService() {
			return () -> "@Bean";
		}
	}

}
