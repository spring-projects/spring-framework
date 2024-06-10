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

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.bean.override.BeanOverrideContextCustomizerTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that validate the behavior of {@link MockitoBean} and
 * {@link MockitoSpyBean} with the TCF context cache.
 *
 * @author Stephane Nicoll
 */
class MockitoBeanContextCustomizerEqualityTests {


	@Test
	void contextCustomizerWithSameMockInDifferentClassIsEqual() {
		assertThat(createContextCustomizer(Case1.class)).isEqualTo(createContextCustomizer(Case2.class));
	}

	@Test
	void contextCustomizerWithSameSpyInDifferentClassIsEqual() {
		assertThat(createContextCustomizer(Case4.class)).isEqualTo(createContextCustomizer(Case5.class));
	}

	@Test
	void contextCustomizerWithSimilarMockButDifferentAnswersIsNotEqual() {
		assertThat(createContextCustomizer(Case1.class)).isNotEqualTo(createContextCustomizer(Case3.class));
	}

	@Test
	void contextCustomizerWithSimilarSpyButDifferentProxyTargetClassFlagIsNotEqual() {
		assertThat(createContextCustomizer(Case5.class)).isNotEqualTo(createContextCustomizer(Case6.class));
	}

	@Test
	void contextCustomizerWithMockAndSpyAreNotEqual() {
		assertThat(createContextCustomizer(Case1.class)).isNotEqualTo(createContextCustomizer(Case4.class));
	}

	private ContextCustomizer createContextCustomizer(Class<?> testClass) {
		ContextCustomizer customizer = BeanOverrideContextCustomizerTestUtils.createContextCustomizer(testClass);
		assertThat(customizer).isNotNull();
		return customizer;
	}

	static class Case1 {

		@MockitoBean
		private String exampleService;

	}

	static class Case2 {

		@MockitoBean
		private String serviceToMock;

	}

	static class Case3 {

		@MockitoBean(answers = Answers.RETURNS_MOCKS)
		private String exampleService;

	}

	static class Case4 {

		@MockitoSpyBean
		private String exampleService;

	}

	static class Case5 {

		@MockitoSpyBean
		private String serviceToMock;

	}

	static class Case6 {

		@MockitoSpyBean(proxyTargetAware = false)
		private String serviceToMock;

	}

}
