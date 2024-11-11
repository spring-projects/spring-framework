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

import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.bean.override.BeanOverrideContextCustomizerTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_MOCKS;

/**
 * Tests that validate the behavior of {@link MockitoBean} and
 * {@link MockitoSpyBean} with the TCF context cache.
 *
 * @author Stephane Nicoll
 */
class MockitoBeanContextCustomizerEqualityTests {

	@Test
	void contextCustomizerWithSameMockByNameInDifferentClassIsEqual() {
		assertThat(customizerFor(Case1ByName.class)).isEqualTo(customizerFor(Case2ByName.class));
	}

	@Test
	void contextCustomizerWithSameMockByTypeInDifferentClassIsEqual() {
		assertThat(customizerFor(Case1ByType.class)).isEqualTo(customizerFor(Case2ByTypeSameFieldName.class));
	}

	@Test
	void contextCustomizerWithSameMockByTypeAndDifferentFieldNamesAreNotEqual() {
		assertThat(customizerFor(Case1ByType.class)).isNotEqualTo(customizerFor(Case2ByType.class));
	}

	@Test
	void contextCustomizerWithSameSpyByNameInDifferentClassIsEqual() {
		assertThat(customizerFor(Case4ByName.class)).isEqualTo(customizerFor(Case5ByName.class));
	}

	@Test
	void contextCustomizerWithSameSpyByTypeInDifferentClassIsEqual() {
		assertThat(customizerFor(Case4ByType.class)).isEqualTo(customizerFor(Case5ByTypeSameFieldName.class));
	}

	@Test
	void contextCustomizerWithSameSpyByTypeAndDifferentFieldNamesAreNotEqual() {
		assertThat(customizerFor(Case4ByType.class)).isNotEqualTo(customizerFor(Case5ByType.class));
	}

	@Test
	void contextCustomizerWithSimilarMockButDifferentAnswersIsNotEqual() {
		assertThat(customizerFor(Case1ByType.class)).isNotEqualTo(customizerFor(Case3.class));
	}

	@Test
	void contextCustomizerWithMockAndSpyAreNotEqual() {
		assertThat(customizerFor(Case1ByType.class)).isNotEqualTo(customizerFor(Case4ByType.class));
	}

	private ContextCustomizer customizerFor(Class<?> testClass) {
		ContextCustomizer customizer = BeanOverrideContextCustomizerTestUtils.createContextCustomizer(testClass);
		assertThat(customizer).isNotNull();
		return customizer;
	}

	static class Case1ByName {

		@MockitoBean("serviceBean")
		private String exampleService;

	}

	static class Case1ByType {

		@MockitoBean
		private String exampleService;

	}

	static class Case2ByName {

		@MockitoBean("serviceBean")
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

		@MockitoBean(answers = RETURNS_MOCKS)
		private String exampleService;

	}

	static class Case4ByName {

		@MockitoSpyBean("serviceBean")
		private String exampleService;

	}

	static class Case4ByType {

		@MockitoSpyBean
		private String exampleService;

	}

	static class Case5ByName {

		@MockitoSpyBean("serviceBean")
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

}
