/*
 * Copyright 2002-2025 the original author or authors.
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
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.bean.override.BeanOverrideHandler;
import org.springframework.test.context.bean.override.BeanOverrideTestUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockitoBeanOverrideHandler}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
class MockitoBeanOverrideHandlerTests {

	@Test
	void beanNameIsSetToNullIfAnnotationNameIsEmpty() {
		List<BeanOverrideHandler> list = BeanOverrideTestUtils.findHandlers(SampleOneMock.class);
		assertThat(list).singleElement().satisfies(handler -> assertThat(handler.getBeanName()).isNull());
	}

	@Test
	void beanNameIsSetToAnnotationName() {
		List<BeanOverrideHandler> list = BeanOverrideTestUtils.findHandlers(SampleOneMockWithName.class);
		assertThat(list).singleElement().satisfies(handler -> assertThat(handler.getBeanName()).isEqualTo("anotherService"));
	}

	@Test
	void isEqualToWithSameInstanceFromField() {
		MockitoBeanOverrideHandler handler = createHandler(sampleField("service"));
		assertThat(handler).isEqualTo(handler);
		assertThat(handler).hasSameHashCodeAs(handler);
	}

	@Test
	void isEqualToWithSameMetadataFromField() {
		MockitoBeanOverrideHandler handler1 = createHandler(sampleField("service"));
		MockitoBeanOverrideHandler handler2 = createHandler(sampleField("service"));
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test  // gh-33925
	void isEqualToWithSameInstanceFromClassLevel() {
		MockitoBeanOverrideHandler handler1 = createHandler(ClassLevelStringMockByName1.class);
		assertThat(handler1).isEqualTo(handler1);
		assertThat(handler1).hasSameHashCodeAs(handler1);

		MockitoBeanOverrideHandler handler2 = createHandler(ClassLevelStringMockByType1.class);
		assertThat(handler2).isEqualTo(handler2);
		assertThat(handler2).hasSameHashCodeAs(handler2);
	}

	@Test  // gh-33925
	void isEqualToWithSameByNameLookupMetadataFromClassLevel() {
		MockitoBeanOverrideHandler handler1 = createHandler(ClassLevelStringMockByName1.class);
		MockitoBeanOverrideHandler handler2 = createHandler(ClassLevelStringMockByName2.class);
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler2).isEqualTo(handler1);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test  // gh-33925
	void isNotEqualToWithDifferentByNameLookupMetadataFromClassLevel() {
		MockitoBeanOverrideHandler handler1 = createHandler(ClassLevelStringMockByName1.class);
		MockitoBeanOverrideHandler handler2 = createHandler(ClassLevelStringMockByName3.class);
		assertThat(handler1).isNotEqualTo(handler2);
		assertThat(handler2).isNotEqualTo(handler1);
		assertThat(handler1).doesNotHaveSameHashCodeAs(handler2);
	}

	@Test  // gh-33925
	void isEqualToWithSameByTypeLookupMetadataFromClassLevel() {
		MockitoBeanOverrideHandler handler1 = createHandler(ClassLevelStringMockByType1.class);
		MockitoBeanOverrideHandler handler2 = createHandler(ClassLevelStringMockByType2.class);
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler2).isEqualTo(handler1);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test  // gh-33925
	void isNotEqualToWithDifferentByTypeLookupMetadataFromClassLevel() {
		MockitoBeanOverrideHandler handler1 = createHandler(ClassLevelStringMockByType1.class);
		MockitoBeanOverrideHandler handler2 = createHandler(ClassLevelStringMockByType3.class);
		assertThat(handler1).isNotEqualTo(handler2);
		assertThat(handler2).isNotEqualTo(handler1);
		assertThat(handler1).doesNotHaveSameHashCodeAs(handler2);
	}

	@Test  // gh-33925
	void isEqualToWithSameByNameLookupMetadataFromFieldAndClassLevel() {
		MockitoBeanOverrideHandler handler1 = createHandler(sampleField("service3"));
		MockitoBeanOverrideHandler handler2 = createHandler(ClassLevelStringMockByName1.class);
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler2).isEqualTo(handler1);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	/**
	 * Since the "field name as fallback qualifier" is not available for an annotated class,
	 * what would seem to be "equivalent" handlers are actually not considered "equal" when
	 * the the lookup is "by type".
	 */
	@Test  // gh-33925
	void isNotEqualToWithSameByTypeLookupMetadataFromFieldAndClassLevel() {
		MockitoBeanOverrideHandler handler1 = createHandler(sampleField("service"));
		MockitoBeanOverrideHandler handler2 = createHandler(ClassLevelStringMockByType1.class);
		assertThat(handler1).isNotEqualTo(handler2);
		assertThat(handler2).isNotEqualTo(handler1);
		assertThat(handler1).doesNotHaveSameHashCodeAs(handler2);
	}

	@Test
	void isNotEqualEqualToByTypeLookupWithSameMetadataButDifferentField() {
		MockitoBeanOverrideHandler handler1 = createHandler(sampleField("service"));
		MockitoBeanOverrideHandler handler2 = createHandler(sampleField("service2"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isEqualEqualToByNameLookupWithSameMetadataButDifferentField() {
		MockitoBeanOverrideHandler handler1 = createHandler(sampleField("service3"));
		MockitoBeanOverrideHandler handler2 = createHandler(sampleField("service4"));
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentBeanName() {
		MockitoBeanOverrideHandler handler1 = createHandler(sampleField("service"));
		MockitoBeanOverrideHandler handler2 = createHandler(sampleField("service3"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentExtraInterfaces() {
		MockitoBeanOverrideHandler handler1 = createHandler(sampleField("service"));
		MockitoBeanOverrideHandler handler2 = createHandler(sampleField("service5"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentAnswers() {
		MockitoBeanOverrideHandler handler1 = createHandler(sampleField("service"));
		MockitoBeanOverrideHandler handler2 = createHandler(sampleField("service6"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentSerializableFlag() {
		MockitoBeanOverrideHandler handler1 = createHandler(sampleField("service"));
		MockitoBeanOverrideHandler handler2 = createHandler(sampleField("service7"));
		assertThat(handler1).isNotEqualTo(handler2);
	}


	private static Field sampleField(String fieldName) {
		Field field = ReflectionUtils.findField(Sample.class, fieldName);
		assertThat(field).isNotNull();
		return field;
	}

	private static MockitoBeanOverrideHandler createHandler(Field field) {
		MockitoBean annotation = AnnotatedElementUtils.getMergedAnnotation(field, MockitoBean.class);
		return new MockitoBeanOverrideHandler(field, ResolvableType.forClass(field.getType()), annotation);
	}

	private MockitoBeanOverrideHandler createHandler(Class<?> clazz) {
		MockitoBean annotation = AnnotatedElementUtils.getMergedAnnotation(clazz, MockitoBean.class);
		return new MockitoBeanOverrideHandler(null, ResolvableType.forClass(annotation.types()[0]), annotation);
	}


	static class SampleOneMock {

		@MockitoBean
		String service;
	}

	static class SampleOneMockWithName {

		@MockitoBean("anotherService")
		String service;
	}

	static class Sample {

		@MockitoBean
		private String service;

		@MockitoBean
		private String service2;

		@MockitoBean(name = "beanToMock")
		private String service3;

		@MockitoBean(value = "beanToMock")
		private String service4;

		@MockitoBean(extraInterfaces = Externalizable.class)
		private String service5;

		@MockitoBean(answers = Answers.RETURNS_MOCKS)
		private String service6;

		@MockitoBean(serializable = true)
		private String service7;
	}

	@MockitoBean(name = "beanToMock", types = String.class)
	static class ClassLevelStringMockByName1 {
	}

	@MockitoBean(name = "beanToMock", types = String.class)
	static class ClassLevelStringMockByName2 {
	}

	@MockitoBean(name = "otherBeanToMock", types = String.class)
	static class ClassLevelStringMockByName3 {
	}

	@MockitoBean(types = String.class)
	static class ClassLevelStringMockByType1 {
	}

	@MockitoBean(types = String.class)
	static class ClassLevelStringMockByType2 {
	}

	@MockitoBean(types = Integer.class)
	static class ClassLevelStringMockByType3 {
	}

}
