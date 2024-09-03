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

package org.springframework.core.annotation;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AttributeMethods}.
 *
 * @author Phillip Webb
 */
class AttributeMethodsTests {

	@Test
	void forAnnotationTypeWhenNullReturnsNone() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(null);
		assertThat(methods).isSameAs(AttributeMethods.NONE);
	}

	@Test
	void forAnnotationTypeWhenHasNoAttributesReturnsNone() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(NoAttributes.class);
		assertThat(methods).isSameAs(AttributeMethods.NONE);
	}

	@Test
	void forAnnotationTypeWhenHasMultipleAttributesReturnsAttributes() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
		assertThat(methods.get("value").getName()).isEqualTo("value");
		assertThat(methods.get("intValue").getName()).isEqualTo("intValue");
		assertThat(getAll(methods)).flatExtracting(Method::getName).containsExactly("intValue", "value");
	}

	@Test
	void indexOfNameReturnsIndex() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
		assertThat(methods.indexOf("value")).isEqualTo(1);
	}

	@Test
	void indexOfMethodReturnsIndex() throws Exception {
		AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
		Method method = MultipleAttributes.class.getDeclaredMethod("value");
		assertThat(methods.indexOf(method)).isEqualTo(1);
	}

	@Test
	void sizeReturnsSize() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
		assertThat(methods.size()).isEqualTo(2);
	}

	@Test
	void canThrowTypeNotPresentExceptionWhenHasClassAttributeReturnsTrue() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(ClassValue.class);
		assertThat(methods.canThrowTypeNotPresentException(0)).isTrue();
	}

	@Test
	void canThrowTypeNotPresentExceptionWhenHasClassArrayAttributeReturnsTrue() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(ClassArrayValue.class);
		assertThat(methods.canThrowTypeNotPresentException(0)).isTrue();
	}

	@Test
	void canThrowTypeNotPresentExceptionWhenNotClassOrClassArrayAttributeReturnsFalse() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(ValueOnly.class);
		assertThat(methods.canThrowTypeNotPresentException(0)).isFalse();
	}

	@Test
	void hasDefaultValueMethodWhenHasDefaultValueMethodReturnsTrue() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(DefaultValueAttribute.class);
		assertThat(methods.hasDefaultValueMethod()).isTrue();
	}

	@Test
	void hasDefaultValueMethodWhenHasNoDefaultValueMethodsReturnsFalse() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
		assertThat(methods.hasDefaultValueMethod()).isFalse();
	}

	@Test
	void isValidWhenHasTypeNotPresentExceptionReturnsFalse() {
		ClassValue annotation = mockAnnotation(ClassValue.class);
		given(annotation.value()).willThrow(TypeNotPresentException.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(annotation.annotationType());
		assertThat(attributes.canLoad(annotation)).isFalse();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void isValidWhenDoesNotHaveTypeNotPresentExceptionReturnsTrue() {
		ClassValue annotation = mock();
		given(annotation.value()).willReturn((Class) InputStream.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(annotation.annotationType());
		assertThat(attributes.canLoad(annotation)).isTrue();
	}

	@Test
	void validateWhenHasTypeNotPresentExceptionThrowsException() {
		ClassValue annotation = mockAnnotation(ClassValue.class);
		given(annotation.value()).willThrow(TypeNotPresentException.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(annotation.annotationType());
		assertThatIllegalStateException().isThrownBy(() -> attributes.validate(annotation));
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void validateWhenDoesNotHaveTypeNotPresentExceptionThrowsNothing() {
		ClassValue annotation = mockAnnotation(ClassValue.class);
		given(annotation.value()).willReturn((Class) InputStream.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(annotation.annotationType());
		attributes.validate(annotation);
	}

	private List<Method> getAll(AttributeMethods attributes) {
		List<Method> result = new ArrayList<>(attributes.size());
		for (int i = 0; i < attributes.size(); i++) {
			result.add(attributes.get(i));
		}
		return result;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <A extends Annotation> A mockAnnotation(Class<A> annotationType) {
		A annotation = mock(annotationType);
		given(annotation.annotationType()).willReturn((Class) annotationType);
		return annotation;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NoAttributes {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface MultipleAttributes {

		int intValue();

		String value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ValueOnly {

		String value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NonValueOnly {

		String test();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ClassValue {

		Class<?> value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ClassArrayValue {

		Class<?>[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface DefaultValueAttribute {

		String one();

		String two();

		String three() default "3";

	}

}
