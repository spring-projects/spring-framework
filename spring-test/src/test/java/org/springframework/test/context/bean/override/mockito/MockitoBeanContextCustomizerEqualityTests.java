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
	void contextCustomizerWithSameMockByNameInDifferentClassIsEqual() {
		assertThat(createContextCustomizer(Case1ByName.class)).isEqualTo(createContextCustomizer(Case2ByName.class));
	}

	@Test
	void contextCustomizerWithSameMockByTypeInDifferentClassIsEqual() {
		assertThat(createContextCustomizer(Case1ByType.class)).isEqualTo(createContextCustomizer(Case2ByTypeSameFieldName.class));
	}

	@Test
	void contextCustomizerWithSameMockByTypeAndDifferentFieldNamesAreNotEqual() {
		assertThat(createContextCustomizer(Case1ByType.class)).isNotEqualTo(createContextCustomizer(Case2ByType.class));
	}

	@Test
	void contextCustomizerWithSameSpyByNameInDifferentClassIsEqual() {
		assertThat(createContextCustomizer(Case4ByName.class)).isEqualTo(createContextCustomizer(Case5ByName.class));
	}

	@Test
	void contextCustomizerWithSameSpyByTypeInDifferentClassIsEqual() {
		assertThat(createContextCustomizer(Case4ByType.class)).isEqualTo(createContextCustomizer(Case5ByTypeSameFieldName.class));
	}

	@Test
	void contextCustomizerWithSameSpyByTypeAndDifferentFieldNamesAreNotEqual() {
		assertThat(createContextCustomizer(Case4ByType.class)).isNotEqualTo(createContextCustomizer(Case5ByType.class));
	}

	@Test
	void contextCustomizerWithSimilarMockButDifferentAnswersIsNotEqual() {
		assertThat(createContextCustomizer(Case1ByType.class)).isNotEqualTo(createContextCustomizer(Case3.class));
	}

	@Test
	void contextCustomizerWithSimilarSpyButDifferentProxyTargetClassFlagIsNotEqual() {
		assertThat(createContextCustomizer(Case5ByType.class)).isNotEqualTo(createContextCustomizer(Case6.class));
	}

	@Test
	void contextCustomizerWithMockAndSpyAreNotEqual() {
		assertThat(createContextCustomizer(Case1ByType.class)).isNotEqualTo(createContextCustomizer(Case4ByType.class));
	}

	private ContextCustomizer createContextCustomizer(Class<?> testClass) {
		ContextCustomizer customizer = BeanOverrideContextCustomizerTestUtils.createContextCustomizer(testClass);
		assertThat(customizer).isNotNull();
		return customizer;
	}

	static class Case1ByName {

		@MockitoBean(name = "serviceBean")
		private String exampleService;

	}

	static class Case1ByType {

		@MockitoBean
		private String exampleService;

	}

	static class Case2ByName {

		@MockitoBean(name = "serviceBean")
		private String serviceToMock;

	}

	static class Case2ByType {

		@MockitoBean
		private String serviceToMock;

	}

	static class Case2ByTypeSameFieldName {

		@MockitoBean
		private String exampleService;

	}

	static class Case3 {

		@MockitoBean(answers = Answers.RETURNS_MOCKS)
		private String exampleService;

	}

	static class Case4ByName {

		@MockitoSpyBean(name = "serviceBean")
		private String exampleService;

	}

	static class Case4ByType {

		@MockitoSpyBean
		private String exampleService;

	}

	static class Case5ByName {

		@MockitoSpyBean(name = "serviceBean")
		private String serviceToMock;

	}

	static class Case5ByType {

		@MockitoSpyBean
		private String serviceToMock;

	}

	static class Case5ByTypeSameFieldName {

		@MockitoSpyBean
		private String exampleService;

	}

	static class Case6 {

		@MockitoSpyBean(proxyTargetAware = false)
		private String serviceToMock;

	}

}
