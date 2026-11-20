/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.convention;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.bean.override.BeanOverrideContextCustomizerTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that validate the behavior of {@link TestBean} with the TCF context cache.
 *
 * @author Stephane Nicoll
 */
class TestBeanContextCustomizerEqualityTests {

	@Test
	void contextCustomizerWithSameOverrideInDifferentTestClassesIsEqual() {
		assertThat(createContextCustomizer(Case1.class)).isEqualTo(createContextCustomizer(Case2.class));
	}

	@Test
	void contextCustomizerWithDifferentMethodsIsNotEqual() {
		assertThat(createContextCustomizer(Case1.class)).isNotEqualTo(createContextCustomizer(Case3.class));
	}

	@Test
	void contextCustomizerWithByNameVsByTypeLookupIsNotEqual() {
		assertThat(createContextCustomizer(Case4.class)).isNotEqualTo(createContextCustomizer(Case5.class));
	}


	private static ContextCustomizer createContextCustomizer(Class<?> testClass) {
		ContextCustomizer customizer = BeanOverrideContextCustomizerTestUtils.createContextCustomizer(testClass);
		assertThat(customizer).isNotNull();
		return customizer;
	}

	interface DescriptionProvider {

		static String createDescription() {
			return "override";
		}

	}

	static class Case1 implements DescriptionProvider {

		@TestBean(methodName = "createDescription")
		private String description;

	}

	static class Case2 implements DescriptionProvider {

		@TestBean(methodName = "createDescription")
		private String description;

	}

	static class Case3 implements DescriptionProvider {

		@TestBean(methodName = "createDescription")
		private String description;

		static String createDescription() {
			return "another value";
		}
	}

	static class Case4 {

		@TestBean
		private String description;

		static String description() {
			return "overridden";
		}
	}

	static class Case5 {

		@TestBean(name = "descriptionBean")
		private String description;

		static String description() {
			return "overridden";
		}
	}

}
