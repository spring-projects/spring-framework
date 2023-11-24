/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Bean @Bean} 'lite' mode features that are not covered
 * elsewhere in the test suite.
 *
 * @author Sam Brannen
 * @since 6.0.10
 * @see ConfigurationClassPostProcessorTests
 */
class BeanLiteModeTests {

	@Test
	void beanMethodsAreFoundWhenInheritedAsInterfaceDefaultMethods() {
		assertBeansAreFound(InterfaceDefaultMethodsConfig.class);
	}

	@Test
	void beanMethodsAreFoundWhenDeclaredLocally() {
		assertBeansAreFound(BaseConfig.class);
	}

	@Test
	void beanMethodsAreFoundWhenDeclaredLocallyAndInSuperclass() {
		assertBeansAreFound(OverridingConfig.class, "foo", "xyz");
	}

	@Test  // gh-30449
	void beanMethodsAreFoundWhenDeclaredOnlyInSuperclass() {
		assertBeansAreFound(ExtendedConfig.class, "foo", "xyz");
	}

	private static void assertBeansAreFound(Class<?> configClass) {
		assertBeansAreFound(configClass, "foo", "bar");
	}

	private static void assertBeansAreFound(Class<?> configClass, String expected1, String expected2) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass)) {
			String bean1 = context.getBean("bean1", String.class);
			String bean2 = context.getBean("bean2", String.class);

			assertThat(bean1).isEqualTo(expected1);
			assertThat(bean2).isEqualTo(expected2);
		}
	}


	interface ConfigInterface {

		@Bean
		default String bean1() {
			return "foo";
		}

		@Bean
		default String bean2() {
			return "bar";
		}
	}

	static class InterfaceDefaultMethodsConfig implements ConfigInterface {
	}

	static class BaseConfig {

		@Bean
		String bean1() {
			return "foo";
		}

		@Bean
		String bean2() {
			return "bar";
		}
	}

	static class OverridingConfig extends BaseConfig {

		@Bean
		@Override
		String bean2() {
			return "xyz";
		}
	}

	static class ExtendedConfig extends OverridingConfig {
	}

}
