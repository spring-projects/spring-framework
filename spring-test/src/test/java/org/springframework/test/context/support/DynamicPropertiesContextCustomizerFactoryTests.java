/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
