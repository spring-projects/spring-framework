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

import java.io.Externalizable;
import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockitoBeanOverrideMetadata}.
 *
 * @author Stephane Nicoll
 */
class MockitoBeanOverrideMetadataTests {

	@Test
	void forTestClassSetsNameToNullIfAnnotationNameIsNull() {
		List<OverrideMetadata> list = OverrideMetadata.forTestClass(SampleOneMock.class);
		assertThat(list).singleElement().satisfies(metadata -> assertThat(metadata.getBeanName()).isNull());
	}

	@Test
	void forTestClassSetsNameToAnnotationName() {
		List<OverrideMetadata> list = OverrideMetadata.forTestClass(SampleOneMockWithName.class);
		assertThat(list).singleElement().satisfies(metadata -> assertThat(metadata.getBeanName()).isEqualTo("anotherService"));
	}

	@Test
	void isEqualToWithSameInstance() {
		MockitoBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		assertThat(metadata).isEqualTo(metadata);
		assertThat(metadata).hasSameHashCodeAs(metadata);
	}

	@Test
	void isEqualToWithSameMetadata() {
		MockitoBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		MockitoBeanOverrideMetadata metadata2 = createMetadata(sampleField("service"));
		assertThat(metadata).isEqualTo(metadata2);
		assertThat(metadata).hasSameHashCodeAs(metadata2);
	}

	@Test
	void isNotEqualEqualToByTypeLookupWithSameMetadataButDifferentField() {
		MockitoBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		MockitoBeanOverrideMetadata metadata2 = createMetadata(sampleField("service2"));
		assertThat(metadata).isNotEqualTo(metadata2);
	}

	@Test
	void isEqualEqualToByNameLookupWithSameMetadataButDifferentField() {
		MockitoBeanOverrideMetadata metadata = createMetadata(sampleField("service3"));
		MockitoBeanOverrideMetadata metadata2 = createMetadata(sampleField("service4"));
		assertThat(metadata).isEqualTo(metadata2);
		assertThat(metadata).hasSameHashCodeAs(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentBeanName() {
		MockitoBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		MockitoBeanOverrideMetadata metadata2 = createMetadata(sampleField("service3"));
		assertThat(metadata).isNotEqualTo(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentExtraInterfaces() {
		MockitoBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		MockitoBeanOverrideMetadata metadata2 = createMetadata(sampleField("service5"));
		assertThat(metadata).isNotEqualTo(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentAnswers() {
		MockitoBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		MockitoBeanOverrideMetadata metadata2 = createMetadata(sampleField("service6"));
		assertThat(metadata).isNotEqualTo(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentSerializableFlag() {
		MockitoBeanOverrideMetadata metadata = createMetadata(sampleField("service"));
		MockitoBeanOverrideMetadata metadata2 = createMetadata(sampleField("service7"));
		assertThat(metadata).isNotEqualTo(metadata2);
	}


	private Field sampleField(String fieldName) {
		Field field = ReflectionUtils.findField(Sample.class, fieldName);
		assertThat(field).isNotNull();
		return field;
	}

	private MockitoBeanOverrideMetadata createMetadata(Field field) {
		MockitoBean annotation = field.getAnnotation(MockitoBean.class);
		return new MockitoBeanOverrideMetadata(field, ResolvableType.forClass(field.getType()), annotation);
	}


	static class SampleOneMock {

		@MockitoBean
		String service;

	}

	static class SampleOneMockWithName {

		@MockitoBean(name = "anotherService")
		String service;

	}

	static class Sample {

		@MockitoBean
		private String service;

		@MockitoBean
		private String service2;

		@MockitoBean(name = "beanToMock")
		private String service3;

		@MockitoBean(name = "beanToMock")
		private String service4;

		@MockitoBean(extraInterfaces = Externalizable.class)
		private String service5;

		@MockitoBean(answers = Answers.RETURNS_MOCKS)
		private String service6;

		@MockitoBean(serializable = true)
		private String service7;

	}

}
