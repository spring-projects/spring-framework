/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito.mockbeans;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.BeanOverrideHandler;
import org.springframework.test.context.bean.override.BeanOverrideTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockitoBeans @MockitoBeans}: {@link MockitoBean @MockitoBean}
 * declared at the class level, as a repeatable annotation, and via a custom composed
 * annotation.
 *
 * @author Sam Brannen
 * @since 6.2.2
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/33925">gh-33925</a>
 */
class MockitoBeansTests {

	@Test
	void registrationOrderForTopLevelClass() {
		Stream<Class<?>> mockedServices = getRegisteredMockTypes(MockitoBeansByTypeIntegrationTests.class);
		assertThat(mockedServices).containsExactly(
				Service01.class, Service02.class, Service03.class, Service04.class,
				Service05.class, Service06.class, Service07.class);
	}

	@Test
	void registrationOrderForNestedClass() {
		Stream<Class<?>> mockedServices = getRegisteredMockTypes(MockitoBeansByTypeIntegrationTests.NestedTests.class);
		assertThat(mockedServices).containsExactly(
				Service01.class, Service02.class, Service03.class, Service04.class,
				Service05.class, Service06.class, Service07.class, Service08.class,
				Service09.class, Service10.class, Service11.class, Service12.class,
				Service13.class);
	}


	private static Stream<Class<?>> getRegisteredMockTypes(Class<?> testClass) {
		return BeanOverrideTestUtils.findAllHandlers(testClass)
				.stream()
				.map(BeanOverrideHandler::getBeanType)
				.map(ResolvableType::getRawClass);
	}

}
