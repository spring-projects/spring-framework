/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.typelevel;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.BeanOverrideHandler;
import org.springframework.test.context.bean.override.BeanOverrideUtils;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBeans;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockitoSpyBeans @MockitoSpyBeans}: {@link MockitoSpyBean @MockitoSpyBean}
 * declared at the class level, as a repeatable annotation, and via a custom composed
 * annotation.
 *
 * @author Sam Brannen
 * @since 6.2.3
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34408">gh-34408</a>
 */
class MockitoSpyBeansTests {

	@Test
	void registrationOrderForTopLevelClass() {
		Stream<Class<?>> mockedServices = getRegisteredMockTypes(MockitoSpyBeansByTypeIntegrationTests.class);
		assertThat(mockedServices).containsExactly(
				Service01.class, Service02.class, Service03.class, Service04.class,
				Service05.class, Service06.class, Service07.class);
	}

	@Test
	void registrationOrderForNestedClass() {
		Stream<Class<?>> mockedServices = getRegisteredMockTypes(MockitoSpyBeansByTypeIntegrationTests.NestedTests.class);
		assertThat(mockedServices).containsExactly(
				Service01.class, Service02.class, Service03.class, Service04.class,
				Service05.class, Service06.class, Service07.class, Service08.class,
				Service09.class, Service10.class, Service11.class, Service12.class,
				Service13.class);
	}


	private static Stream<Class<?>> getRegisteredMockTypes(Class<?> testClass) {
		return BeanOverrideUtils.findAllHandlers(testClass)
				.stream()
				.map(BeanOverrideHandler::getBeanType)
				.map(ResolvableType::getRawClass);
	}

}
