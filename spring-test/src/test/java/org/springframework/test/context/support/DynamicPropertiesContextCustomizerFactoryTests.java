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

package org.springframework.test.context.support;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynamicPropertiesContextCustomizerFactory}.
 *
 * @author Phillip Webb
 */
class DynamicPropertiesContextCustomizerFactoryTests {

	private final DynamicPropertiesContextCustomizerFactory factory = new DynamicPropertiesContextCustomizerFactory();

	private final List<ContextConfigurationAttributes> configAttributes = Collections.emptyList();

	@Test
	void createContextCustomizerWhenNoAnnotatedMethodsReturnsCustomizerWithEmptyMethods() {
		DynamicPropertiesContextCustomizer customizer = this.factory.createContextCustomizer(
				NoDynamicPropertySource.class, this.configAttributes);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getMethods()).isEmpty();
	}

	@Test
	void createContextCustomizerWhenSingleAnnotatedMethodReturnsCustomizer() {
		DynamicPropertiesContextCustomizer customizer = this.factory.createContextCustomizer(
			SingleDynamicPropertySource.class, this.configAttributes);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getMethods()).flatExtracting(Method::getName).containsOnly("p1");
	}

	@Test
	void createContextCustomizerWhenMultipleAnnotatedMethodsReturnsCustomizer() {
		DynamicPropertiesContextCustomizer customizer = this.factory.createContextCustomizer(
			MultipleDynamicPropertySources.class, this.configAttributes);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getMethods()).flatExtracting(Method::getName).containsOnly("p1", "p2", "p3");
	}

	@Test
	void createContextCustomizerWhenAnnotatedMethodsInBaseClassReturnsCustomizer() {
		DynamicPropertiesContextCustomizer customizer = this.factory.createContextCustomizer(
			SubDynamicPropertySource.class, this.configAttributes);
		assertThat(customizer).isNotNull();
		assertThat(customizer.getMethods()).flatExtracting(Method::getName).containsOnly("p1", "p2");
	}


	static class NoDynamicPropertySource {

		void empty() {
		}

	}

	static class SingleDynamicPropertySource {

		@DynamicPropertySource
		static void p1(DynamicPropertyRegistry registry) {
		}

	}

	static class MultipleDynamicPropertySources {

		@DynamicPropertySource
		static void p1(DynamicPropertyRegistry registry) {
		}

		@DynamicPropertySource
		static void p2(DynamicPropertyRegistry registry) {
		}

		@DynamicPropertySource
		static void p3(DynamicPropertyRegistry registry) {
		}

	}

	static class BaseDynamicPropertySource {

		@DynamicPropertySource
		static void p1(DynamicPropertyRegistry registry) {
		}

	}

	static class SubDynamicPropertySource extends BaseDynamicPropertySource {

		@DynamicPropertySource
		static void p2(DynamicPropertyRegistry registry) {
		}

	}

}
