/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for gh-29105.
 *
 * @author Stephane Nicoll
 */
class Gh29105Tests {

	@Test
	void beanProviderWithParentContextReuseOrder() {
		AnnotationConfigApplicationContext parent =
				new AnnotationConfigApplicationContext(DefaultConfiguration.class, CustomConfiguration.class);

		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.setParent(parent);
		child.register(DefaultConfiguration.class);
		child.refresh();

		Stream<Class<?>> orderedTypes = child.getBeanProvider(MyService.class).orderedStream().map(Object::getClass);
		assertThat(orderedTypes).containsExactly(CustomService.class, DefaultService.class);

		assertThat(child.getDefaultListableBeanFactory().getOrder("defaultService")).isEqualTo(0);
		assertThat(child.getDefaultListableBeanFactory().getOrder("customService")).isEqualTo(-1);

		child.close();
		parent.close();
	}


	interface MyService {}

	static class CustomService implements MyService {}

	static class DefaultService implements MyService {}


	@Configuration
	static class CustomConfiguration {

		@Bean
		@Order(-1)
		CustomService customService() {
			return new CustomService();
		}

	}

	@Configuration
	static class DefaultConfiguration {

		@Bean
		@Order(0)
		DefaultService defaultService() {
			return new DefaultService();
		}

	}

}
