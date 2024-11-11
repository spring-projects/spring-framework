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

package org.springframework.test.context.bean.override.mockito.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.integration.AbstractMockitoBeanAndGenericsIntegrationTests.Something;
import org.springframework.test.context.bean.override.mockito.integration.AbstractMockitoBeanAndGenericsIntegrationTests.Thing;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Abstract base class for tests for {@link MockitoBean @MockitoBean} with generics.
 *
 * @param <T> type of thing
 * @param <S> type of something
 * @author Madhura Bhave
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoBeanAndGenericsIntegrationTests
 */
@SpringJUnitConfig
abstract class AbstractMockitoBeanAndGenericsIntegrationTests<T extends Thing<S>, S extends Something> {

	@Autowired
	T thing;

	@MockitoBean
	S something;


	static class Something {
		String speak() {
			return "Hi";
		}
	}

	static class SomethingImpl extends Something {
	}

	abstract static class Thing<S extends Something> {

		@Autowired
		private S something;

		S getSomething() {
			return this.something;
		}
	}

	static class ThingImpl extends Thing<SomethingImpl> {
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		ThingImpl thing() {
			return new ThingImpl();
		}
	}

}
