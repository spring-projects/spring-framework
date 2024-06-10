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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link TestBeanOverrideMetadata}.
 *
 * @author Stephane Nicoll
 */
class TestBeanOverrideMetadataTests {

	@Test
	void forTestClassSetsNameToNullIfAnnotationNameIsNull() {
		List<OverrideMetadata> list = OverrideMetadata.forTestClass(SampleOneOverride.class);
		assertThat(list).singleElement().satisfies(metadata -> assertThat(metadata.getBeanName()).isNull());
	}

	@Test
	void forTestClassSetsNameToAnnotationName() {
		List<OverrideMetadata> list = OverrideMetadata.forTestClass(SampleOneOverrideWithName.class);
		assertThat(list).singleElement().satisfies(metadata -> assertThat(metadata.getBeanName()).isEqualTo("anotherBean"));
	}

	@Test
	void forTestClassWithMissingMethod() {
		assertThatIllegalStateException()
				.isThrownBy(() ->OverrideMetadata.forTestClass(SampleMissingMethod.class))
				.withMessage("No static method found named message() in %s with return type %s",
						SampleMissingMethod.class.getName(), String.class.getName());
	}

	@Test
	void isEqualToWithSameInstance() {
		TestBeanOverrideMetadata metadata = createMetadata(sampleField("message"), sampleMethod("message"));
		assertThat(metadata).isEqualTo(metadata);
		assertThat(metadata).hasSameHashCodeAs(metadata);
	}

	@Test
	void isEqualToWithSameMetadata() {
		TestBeanOverrideMetadata metadata1 = createMetadata(sampleField("message"), sampleMethod("message"));
		TestBeanOverrideMetadata metadata2 = createMetadata(sampleField("message"), sampleMethod("message"));
		assertThat(metadata1).isEqualTo(metadata2);
		assertThat(metadata1).hasSameHashCodeAs(metadata2);
	}

	@Test
	void isEqualToWithSameMetadataByNameLookupAndDifferentField() {
		TestBeanOverrideMetadata metadata1 = createMetadata(sampleField("message3"), sampleMethod("message"));
		TestBeanOverrideMetadata metadata2 = createMetadata(sampleField("message4"), sampleMethod("message"));
		assertThat(metadata1).isEqualTo(metadata2);
		assertThat(metadata1).hasSameHashCodeAs(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataByTypeLookupAndDifferentField() {
		TestBeanOverrideMetadata metadata1 = createMetadata(sampleField("message"), sampleMethod("message"));
		TestBeanOverrideMetadata metadata2 = createMetadata(sampleField("message2"), sampleMethod("message"));
		assertThat(metadata1).isNotEqualTo(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentBeanName() {
		TestBeanOverrideMetadata metadata1 = createMetadata(sampleField("message"), sampleMethod("message"));
		TestBeanOverrideMetadata metadata2 = createMetadata(sampleField("message3"), sampleMethod("message"));
		assertThat(metadata1).isNotEqualTo(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentMethod() {
		TestBeanOverrideMetadata metadata1 = createMetadata(sampleField("message"), sampleMethod("message"));
		TestBeanOverrideMetadata metadata2 = createMetadata(sampleField("message"), sampleMethod("description"));
		assertThat(metadata1).isNotEqualTo(metadata2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentAnnotations() {
		TestBeanOverrideMetadata metadata1 = createMetadata(sampleField("message"), sampleMethod("message"));
		TestBeanOverrideMetadata metadata2 = createMetadata(sampleField("message5"), sampleMethod("message"));
		assertThat(metadata1).isNotEqualTo(metadata2);
	}

	private Field sampleField(String fieldName) {
		Field field = ReflectionUtils.findField(Sample.class, fieldName);
		assertThat(field).isNotNull();
		return field;
	}

	private Method sampleMethod(String noArgMethodName) {
		Method method = ReflectionUtils.findMethod(Sample.class, noArgMethodName);
		assertThat(method).isNotNull();
		return method;
	}

	private TestBeanOverrideMetadata createMetadata(Field field, Method overrideMethod) {
		TestBean annotation = field.getAnnotation(TestBean.class);
		String beanName = (StringUtils.hasText(annotation.name()) ? annotation.name() : null);
		return new TestBeanOverrideMetadata(field, ResolvableType.forClass(field.getType()), beanName, overrideMethod);
	}

	static class SampleOneOverride {

		@TestBean
		String message;

		static String message() {
			return "OK";
		}

	}

	static class SampleOneOverrideWithName {

		@TestBean(name = "anotherBean")
		String message;

		static String message() {
			return "OK";
		}

	}

	static class SampleMissingMethod {

		@TestBean
		String message;

	}


	@SuppressWarnings("unused")
	static class Sample {

		@TestBean
		private String message;

		@TestBean
		private String message2;

		@TestBean(name = "anotherBean")
		private String message3;

		@TestBean(name = "anotherBean")
		private String message4;

		@Qualifier("anotherBean")
		@TestBean
		private String message5;

		static String message() {
			return "OK";
		}

		static String description() {
			return message();
		}
	}

}
