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

package org.springframework.test.context.bean.override;

import java.util.Collections;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bean override tests that validate the behavior with the TCF context cache.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
class BeanOverrideContextCustomizerEqualityTests {

	private final BeanOverrideContextCustomizerFactory factory = new BeanOverrideContextCustomizerFactory();

	@Nested
	class SameContextTests {

		@Test
		void testsWithOneIdenticalTestBean() {
			assertThat(createContextCustomizer(Case1.class)).isEqualTo(createContextCustomizer(Case2.class));
		}

		@Test
		void testsWithOneIdenticalMockitoMockBean() {
			assertThat(createContextCustomizer(Case4.class)).isEqualTo(createContextCustomizer(Case5.class));
		}

		@Test
		void testsWithOneIdenticalMockitoSpyBean() {
			assertThat(createContextCustomizer(Case7.class)).isEqualTo(createContextCustomizer(Case8.class));
		}
	}

	@Nested
	class DifferentContextTests {

		@Test
		void testsWithSimilarTestBeanButDifferentMethod() {
			assertThat(createContextCustomizer(Case1.class)).isNotEqualTo(createContextCustomizer(Case3.class));
		}

		@Test
		void testsWithSimilarMockitoMockButDifferentAnswers() {
			assertThat(createContextCustomizer(Case4.class)).isNotEqualTo(createContextCustomizer(Case6.class));
		}

		@Test
		void testsWithSimilarMockitoSpyButDifferentProxyTargetClass() {
			assertThat(createContextCustomizer(Case8.class)).isNotEqualTo(createContextCustomizer(Case9.class));
		}

		@Test
		void testsWithSameConfigurationButOneIsMockitoBeanAndTheOtherMockitoSpy() {
			assertThat(createContextCustomizer(Case4.class)).isNotEqualTo(createContextCustomizer(Case7.class));
		}

	}


	private ContextCustomizer createContextCustomizer(Class<?> testClass) {
		BeanOverrideContextCustomizer customizer = this.factory.createContextCustomizer(
				testClass, Collections.emptyList());
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

		@MockitoBean
		private String exampleService;

	}

	static class Case5 {

		@MockitoBean
		private String serviceToMock;

	}

	static class Case6 {

		@MockitoBean(answers = Answers.RETURNS_MOCKS)
		private String exampleService;

	}

	static class Case7 {

		@MockitoSpyBean
		private String exampleService;

	}

	static class Case8 {

		@MockitoSpyBean
		private String serviceToMock;

	}

	static class Case9 {

		@MockitoSpyBean(proxyTargetAware = false)
		private String serviceToMock;

	}

}
