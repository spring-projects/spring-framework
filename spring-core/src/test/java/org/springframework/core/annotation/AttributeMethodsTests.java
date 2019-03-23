/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests for {@link AttributeMethods}.
 *
 * @author Phillip Webb
 */
public class AttributeMethodsTests {

	@Test
	public void forAnnotationTypeWhenNullReturnsNone() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(null);
		assertThat(methods).isSameAs(AttributeMethods.NONE);
	}

	@Test
	public void forAnnotationTypeWhenHasNoAttributesReturnsNone() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(NoAttributes.class);
		assertThat(methods).isSameAs(AttributeMethods.NONE);
	}

	@Test
	public void forAnnotationTypeWhenHasMultipleAttributesReturnsAttributes() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(
				MultipleAttributes.class);
		assertThat(methods.get("value").getName()).isEqualTo("value");
		assertThat(methods.get("intValue").getName()).isEqualTo("intValue");
		assertThat(getAll(methods)).flatExtracting(Method::getName).containsExactly(
				"intValue", "value");
	}

	@Test
	public void isOnlyValueAttributeWhenHasOnlyValueAttributeReturnsTrue() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(ValueOnly.class);
		assertThat(methods.isOnlyValueAttribute()).isTrue();
	}

	@Test
	public void isOnlyValueAttributeWhenHasOnlySingleNonValueAttributeReturnsFalse() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(NonValueOnly.class);
		assertThat(methods.isOnlyValueAttribute()).isFalse();
	}

	@Test
	public void isOnlyValueAttributeWhenHasOnlyMultipleAttributesIncludingValueReturnsFalse() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(
				MultipleAttributes.class);
		assertThat(methods.isOnlyValueAttribute()).isFalse();
	}

	@Test
	public void indexOfNameReturnsIndex() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(
				MultipleAttributes.class);
		assertThat(methods.indexOf("value")).isEqualTo(1);
	}

	@Test
	public void indexOfMethodReturnsIndex() throws Exception {
		AttributeMethods methods = AttributeMethods.forAnnotationType(
				MultipleAttributes.class);
		Method method = MultipleAttributes.class.getDeclaredMethod("value");
		assertThat(methods.indexOf(method)).isEqualTo(1);
	}

	@Test
	public void sizeReturnsSize() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(
				MultipleAttributes.class);
		assertThat(methods.size()).isEqualTo(2);
	}

	@Test
	public void canThrowTypeNotPresentExceptionWhenHasClassAttributeReturnsTrue() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(ClassValue.class);
		assertThat(methods.canThrowTypeNotPresentException(0)).isTrue();
	}

	@Test
	public void canThrowTypeNotPresentExceptionWhenHasClassArrayAttributeReturnsTrue() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(
				ClassArrayValue.class);
		assertThat(methods.canThrowTypeNotPresentException(0)).isTrue();
	}

	@Test
	public void canThrowTypeNotPresentExceptionWhenNotClassOrClassArrayAttributeReturnsFalse() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(ValueOnly.class);
		assertThat(methods.canThrowTypeNotPresentException(0)).isFalse();
	}

	@Test
	public void hasDefaultValueMethodWhenHasDefaultValueMethodReturnsTrue() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(
				DefaultValueAttribute.class);
		assertThat(methods.hasDefaultValueMethod()).isTrue();
	}

	@Test
	public void hasDefaultValueMethodWhenHasNoDefaultValueMethodsReturnsFalse() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(
				MultipleAttributes.class);
		assertThat(methods.hasDefaultValueMethod()).isFalse();
	}

	@Test
	public void isValidWhenHasTypeNotPresentExceptionReturnsFalse() {
		ClassValue annotation = mockAnnotation(ClassValue.class);
		given(annotation.value()).willThrow(TypeNotPresentException.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				annotation.annotationType());
		assertThat(attributes.isValid(annotation)).isFalse();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void isValidWhenDoesNotHaveTypeNotPresentExceptionReturnsTrue() {
		ClassValue annotation = mock(ClassValue.class);
		given(annotation.value()).willReturn((Class) InputStream.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				annotation.annotationType());
		assertThat(attributes.isValid(annotation)).isTrue();
	}

	@Test
	public void validateWhenHasTypeNotPresentExceptionThrowsException() {
		ClassValue annotation = mockAnnotation(ClassValue.class);
		given(annotation.value()).willThrow(TypeNotPresentException.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				annotation.annotationType());
		assertThatIllegalStateException().isThrownBy(
				() -> attributes.validate(annotation));
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void validateWhenDoesNotHaveTypeNotPresentExceptionThrowsNothing() {
		ClassValue annotation = mockAnnotation(ClassValue.class);
		given(annotation.value()).willReturn((Class) InputStream.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				annotation.annotationType());
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
	static @interface NoAttributes {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface MultipleAttributes {

		int intValue();

		String value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ValueOnly {

		String value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface NonValueOnly {

		String test();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ClassValue {

		Class<?> value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ClassArrayValue {

		Class<?>[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface DefaultValueAttribute {

		String one();

		String two();

		String three() default "3";

	}

}
