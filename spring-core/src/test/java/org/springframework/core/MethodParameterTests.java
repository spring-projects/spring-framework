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

package org.springframework.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MethodParameter}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Phillip Webb
 */
class MethodParameterTests {

	private Method method;

	private MethodParameter stringParameter;

	private MethodParameter longParameter;

	private MethodParameter intReturnType;


	@BeforeEach
	void setup() throws NoSuchMethodException {
		method = getClass().getMethod("method", String.class, Long.TYPE);
		stringParameter = new MethodParameter(method, 0);
		longParameter = new MethodParameter(method, 1);
		intReturnType = new MethodParameter(method, -1);
	}


	@Test
	void equals() throws NoSuchMethodException {
		assertThat(stringParameter).isEqualTo(stringParameter);
		assertThat(longParameter).isEqualTo(longParameter);
		assertThat(intReturnType).isEqualTo(intReturnType);

		assertThat(stringParameter.equals(longParameter)).isFalse();
		assertThat(stringParameter.equals(intReturnType)).isFalse();
		assertThat(longParameter.equals(stringParameter)).isFalse();
		assertThat(longParameter.equals(intReturnType)).isFalse();
		assertThat(intReturnType.equals(stringParameter)).isFalse();
		assertThat(intReturnType.equals(longParameter)).isFalse();

		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		MethodParameter methodParameter = new MethodParameter(method, 0);
		assertThat(methodParameter).isEqualTo(stringParameter);
		assertThat(stringParameter).isEqualTo(methodParameter);
		assertThat(methodParameter).isNotEqualTo(longParameter);
		assertThat(longParameter).isNotEqualTo(methodParameter);
	}

	@Test
	void testHashCode() throws NoSuchMethodException {
		assertThat(stringParameter.hashCode()).isEqualTo(stringParameter.hashCode());
		assertThat(longParameter.hashCode()).isEqualTo(longParameter.hashCode());
		assertThat(intReturnType.hashCode()).isEqualTo(intReturnType.hashCode());

		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		MethodParameter methodParameter = new MethodParameter(method, 0);
		assertThat(methodParameter.hashCode()).isEqualTo(stringParameter.hashCode());
		assertThat(methodParameter.hashCode()).isNotEqualTo((long) longParameter.hashCode());
	}

	@Test
	@SuppressWarnings("deprecation")
	void testFactoryMethods() {
		assertThat(MethodParameter.forMethodOrConstructor(method, 0)).isEqualTo(stringParameter);
		assertThat(MethodParameter.forMethodOrConstructor(method, 1)).isEqualTo(longParameter);

		assertThat(MethodParameter.forExecutable(method, 0)).isEqualTo(stringParameter);
		assertThat(MethodParameter.forExecutable(method, 1)).isEqualTo(longParameter);

		assertThat(MethodParameter.forParameter(method.getParameters()[0])).isEqualTo(stringParameter);
		assertThat(MethodParameter.forParameter(method.getParameters()[1])).isEqualTo(longParameter);
	}

	@Test
	void indexValidation() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MethodParameter(method, 2));
	}

	@Test
	void annotatedConstructorParameterInStaticNestedClass() throws Exception {
		Constructor<?> constructor = NestedClass.class.getDeclaredConstructor(String.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(constructor, 0);
		assertThat(methodParameter.getParameterType()).isEqualTo(String.class);
		assertThat(methodParameter.getParameterAnnotation(Param.class)).as("Failed to find @Param annotation").isNotNull();
	}

	@Test  // SPR-16652
	void annotatedConstructorParameterInInnerClass() throws Exception {
		Constructor<?> constructor = InnerClass.class.getConstructor(getClass(), String.class, Callable.class);

		MethodParameter methodParameter = MethodParameter.forExecutable(constructor, 0);
		assertThat(methodParameter.getParameterType()).isEqualTo(getClass());
		assertThat(methodParameter.getParameterAnnotation(Param.class)).isNull();

		methodParameter = MethodParameter.forExecutable(constructor, 1);
		assertThat(methodParameter.getParameterType()).isEqualTo(String.class);
		assertThat(methodParameter.getParameterAnnotation(Param.class)).as("Failed to find @Param annotation").isNotNull();

		methodParameter = MethodParameter.forExecutable(constructor, 2);
		assertThat(methodParameter.getParameterType()).isEqualTo(Callable.class);
		assertThat(methodParameter.getParameterAnnotation(Param.class)).isNull();
	}

	@Test  // SPR-16734
	void genericConstructorParameterInInnerClass() throws Exception {
		Constructor<?> constructor = InnerClass.class.getConstructor(getClass(), String.class, Callable.class);

		MethodParameter methodParameter = MethodParameter.forExecutable(constructor, 0);
		assertThat(methodParameter.getParameterType()).isEqualTo(getClass());
		assertThat(methodParameter.getGenericParameterType()).isEqualTo(getClass());

		methodParameter = MethodParameter.forExecutable(constructor, 1);
		assertThat(methodParameter.getParameterType()).isEqualTo(String.class);
		assertThat(methodParameter.getGenericParameterType()).isEqualTo(String.class);

		methodParameter = MethodParameter.forExecutable(constructor, 2);
		assertThat(methodParameter.getParameterType()).isEqualTo(Callable.class);
		assertThat(methodParameter.getGenericParameterType()).isEqualTo(ResolvableType.forClassWithGenerics(Callable.class, Integer.class).getType());
	}

	@Test
	@Deprecated
	void multipleResolveParameterTypeCalls() throws Exception {
		Method method = ArrayList.class.getMethod("get", int.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(method, -1);
		assertThat(methodParameter.getParameterType()).isEqualTo(Object.class);
		GenericTypeResolver.resolveParameterType(methodParameter, StringList.class);
		assertThat(methodParameter.getParameterType()).isEqualTo(String.class);
		GenericTypeResolver.resolveParameterType(methodParameter, IntegerList.class);
		assertThat(methodParameter.getParameterType()).isEqualTo(Integer.class);
	}

	@Test
	void equalsAndHashCodeConsidersContainingClass() throws Exception {
		Method method = ArrayList.class.getMethod("get", int.class);
		MethodParameter m1 = MethodParameter.forExecutable(method, -1);
		MethodParameter m2 = MethodParameter.forExecutable(method, -1);
		MethodParameter m3 = MethodParameter.forExecutable(method, -1).nested();
		assertThat(m1).isEqualTo(m2).isNotEqualTo(m3);
		assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
	}

	@Test
	void equalsAndHashCodeConsidersNesting() throws Exception {
		Method method = ArrayList.class.getMethod("get", int.class);
		MethodParameter m1 = MethodParameter.forExecutable(method, -1)
				.withContainingClass(StringList.class);
		MethodParameter m2 = MethodParameter.forExecutable(method, -1)
				.withContainingClass(StringList.class);
		MethodParameter m3 = MethodParameter.forExecutable(method, -1)
				.withContainingClass(IntegerList.class);
		MethodParameter m4 = MethodParameter.forExecutable(method, -1);
		assertThat(m1).isEqualTo(m2).isNotEqualTo(m3).isNotEqualTo(m4);
		assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
	}

	@Test
	void withContainingClassReturnsNewInstance() throws Exception {
		Method method = ArrayList.class.getMethod("get", int.class);
		MethodParameter m1 = MethodParameter.forExecutable(method, -1);
		MethodParameter m2 = m1.withContainingClass(StringList.class);
		MethodParameter m3 = m1.withContainingClass(IntegerList.class);
		assertThat(m1).isNotSameAs(m2).isNotSameAs(m3);
		assertThat(m1.getParameterType()).isEqualTo(Object.class);
		assertThat(m2.getParameterType()).isEqualTo(String.class);
		assertThat(m3.getParameterType()).isEqualTo(Integer.class);
	}

	@Test
	void withTypeIndexReturnsNewInstance() throws Exception {
		Method method = ArrayList.class.getMethod("get", int.class);
		MethodParameter m1 = MethodParameter.forExecutable(method, -1);
		MethodParameter m2 = m1.withTypeIndex(2);
		MethodParameter m3 = m1.withTypeIndex(3);
		assertThat(m1).isNotSameAs(m2).isNotSameAs(m3);
		assertThat(m1.getTypeIndexForCurrentLevel()).isNull();
		assertThat(m2.getTypeIndexForCurrentLevel()).isEqualTo(2);
		assertThat(m3.getTypeIndexForCurrentLevel()).isEqualTo(3);
	}

	@Test
	@SuppressWarnings("deprecation")
	void mutatingNestingLevelShouldNotChangeNewInstance() throws Exception {
		Method method = ArrayList.class.getMethod("get", int.class);
		MethodParameter m1 = MethodParameter.forExecutable(method, -1);
		MethodParameter m2 = m1.withTypeIndex(2);
		assertThat(m2.getTypeIndexForCurrentLevel()).isEqualTo(2);
		m1.setTypeIndexForCurrentLevel(1);
		m2.decreaseNestingLevel();
		assertThat(m2.getTypeIndexForCurrentLevel()).isNull();
	}

	@Test
	void nestedWithTypeIndexReturnsNewInstance() throws Exception {
		Method method = ArrayList.class.getMethod("get", int.class);
		MethodParameter m1 = MethodParameter.forExecutable(method, -1);
		MethodParameter m2 = m1.nested(2);
		MethodParameter m3 = m1.nested(3);
		assertThat(m1).isNotSameAs(m2).isNotSameAs(m3);
		assertThat(m1.getTypeIndexForCurrentLevel()).isNull();
		assertThat(m2.getTypeIndexForCurrentLevel()).isEqualTo(2);
		assertThat(m3.getTypeIndexForCurrentLevel()).isEqualTo(3);
	}

	public int method(String p1, long p2) {
		return 42;
	}

	@SuppressWarnings("unused")
	private static class NestedClass {

		NestedClass(@Param String s) {
		}
	}

	@SuppressWarnings("unused")
	private class InnerClass {

		public InnerClass(@Param String s, Callable<Integer> i) {
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	private @interface Param {
	}

	@SuppressWarnings("serial")
	private static class StringList extends ArrayList<String> {
	}

	@SuppressWarnings("serial")
	private static class IntegerList extends ArrayList<Integer> {
	}

}
