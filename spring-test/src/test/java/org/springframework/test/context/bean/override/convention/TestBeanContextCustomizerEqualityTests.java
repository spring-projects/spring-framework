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


	private ContextCustomizer createContextCustomizer(Class<?> testClass) {
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

		private static String description() {
			return "overridden";
		}
	}

	static class Case5 {

		@TestBean(name = "descriptionBean")
		private String description;

		private static String description() {
			return "overridden";
		}
	}

}
