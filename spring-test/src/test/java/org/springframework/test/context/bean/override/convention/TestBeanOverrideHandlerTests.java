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

package org.springframework.test.context.bean.override.convention;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.BeanOverrideHandler;
import org.springframework.test.context.bean.override.BeanOverrideStrategy;
import org.springframework.test.context.bean.override.BeanOverrideTestUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link TestBeanOverrideHandler}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
class TestBeanOverrideHandlerTests {

	@Test
	void beanNameIsSetToNullIfAnnotationNameIsEmpty() {
		List<BeanOverrideHandler> handlers = BeanOverrideTestUtils.findHandlers(SampleOneOverride.class);
		assertThat(handlers).singleElement().extracting(BeanOverrideHandler::getBeanName).isNull();
	}

	@Test
	void beanNameIsSetToAnnotationName() {
		List<BeanOverrideHandler> handlers = BeanOverrideTestUtils.findHandlers(SampleOneOverrideWithName.class);
		assertThat(handlers).singleElement().extracting(BeanOverrideHandler::getBeanName).isEqualTo("anotherBean");
	}

	@Test
	void failsWithMissingMethod() {
		assertThatIllegalStateException()
				.isThrownBy(() -> BeanOverrideTestUtils.findHandlers(SampleMissingMethod.class))
				.withMessage("No static method found named message() in %s with return type %s",
						SampleMissingMethod.class.getName(), String.class.getName());
	}

	@Test
	void isEqualToWithSameInstance() {
		TestBeanOverrideHandler handler = handlerFor(sampleField("message"), sampleMethod("message"));
		assertThat(handler).isEqualTo(handler);
		assertThat(handler).hasSameHashCodeAs(handler);
	}

	@Test
	void isEqualToWithSameMetadata() {
		TestBeanOverrideHandler handler1 = handlerFor(sampleField("message"), sampleMethod("message"));
		TestBeanOverrideHandler handler2 = handlerFor(sampleField("message"), sampleMethod("message"));
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isEqualToWithSameMetadataByNameLookupAndDifferentField() {
		TestBeanOverrideHandler handler1 = handlerFor(sampleField("message3"), sampleMethod("message"));
		TestBeanOverrideHandler handler2 = handlerFor(sampleField("message4"), sampleMethod("message"));
		assertThat(handler1).isEqualTo(handler2);
		assertThat(handler1).hasSameHashCodeAs(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataByTypeLookupAndDifferentField() {
		TestBeanOverrideHandler handler1 = handlerFor(sampleField("message"), sampleMethod("message"));
		TestBeanOverrideHandler handler2 = handlerFor(sampleField("message2"), sampleMethod("message"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentBeanName() {
		TestBeanOverrideHandler handler1 = handlerFor(sampleField("message"), sampleMethod("message"));
		TestBeanOverrideHandler handler2 = handlerFor(sampleField("message3"), sampleMethod("message"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentMethod() {
		TestBeanOverrideHandler handler1 = handlerFor(sampleField("message"), sampleMethod("message"));
		TestBeanOverrideHandler handler2 = handlerFor(sampleField("message"), sampleMethod("description"));
		assertThat(handler1).isNotEqualTo(handler2);
	}

	@Test
	void isNotEqualToWithSameMetadataButDifferentAnnotations() {
		TestBeanOverrideHandler handler1 = handlerFor(sampleField("message"), sampleMethod("message"));
		TestBeanOverrideHandler handler2 = handlerFor(sampleField("message5"), sampleMethod("message"));
		assertThat(handler1).isNotEqualTo(handler2);
	}


	private static Field sampleField(String fieldName) {
		Field field = ReflectionUtils.findField(Sample.class, fieldName);
		assertThat(field).isNotNull();
		return field;
	}

	private static Method sampleMethod(String noArgMethodName) {
		Method method = ReflectionUtils.findMethod(Sample.class, noArgMethodName);
		assertThat(method).isNotNull();
		return method;
	}

	private static TestBeanOverrideHandler handlerFor(Field field, Method overrideMethod) {
		TestBean annotation = field.getAnnotation(TestBean.class);
		String beanName = (StringUtils.hasText(annotation.name()) ? annotation.name() : null);
		return new TestBeanOverrideHandler(
				field, ResolvableType.forClass(field.getType()), beanName, "", BeanOverrideStrategy.REPLACE, overrideMethod);
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
