/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
