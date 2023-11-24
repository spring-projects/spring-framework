/*
 * Copyright 2002-2022 the original author or authors.
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
