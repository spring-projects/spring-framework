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
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class MethodParameterTests {

	private Method method;

	private MethodParameter stringParameter;

	private MethodParameter longParameter;

	private MethodParameter intReturnType;


	@Before
	public void setup() throws NoSuchMethodException {
		method = getClass().getMethod("method", String.class, Long.TYPE);
		stringParameter = new MethodParameter(method, 0);
		longParameter = new MethodParameter(method, 1);
		intReturnType = new MethodParameter(method, -1);
	}


	@Test
	public void testEquals() throws NoSuchMethodException {
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
	public void testHashCode() throws NoSuchMethodException {
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
	public void testFactoryMethods() {
		assertThat(MethodParameter.forMethodOrConstructor(method, 0)).isEqualTo(stringParameter);
		assertThat(MethodParameter.forMethodOrConstructor(method, 1)).isEqualTo(longParameter);

		assertThat(MethodParameter.forExecutable(method, 0)).isEqualTo(stringParameter);
		assertThat(MethodParameter.forExecutable(method, 1)).isEqualTo(longParameter);

		assertThat(MethodParameter.forParameter(method.getParameters()[0])).isEqualTo(stringParameter);
		assertThat(MethodParameter.forParameter(method.getParameters()[1])).isEqualTo(longParameter);
	}

	@Test
	public void testIndexValidation() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MethodParameter(method, 2));
	}

	@Test
	public void annotatedConstructorParameterInStaticNestedClass() throws Exception {
		Constructor<?> constructor = NestedClass.class.getDeclaredConstructor(String.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(constructor, 0);
		assertThat(methodParameter.getParameterType()).isEqualTo(String.class);
		assertThat(methodParameter.getParameterAnnotation(Param.class)).as("Failed to find @Param annotation").isNotNull();
	}

	@Test  // SPR-16652
	public void annotatedConstructorParameterInInnerClass() throws Exception {
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
	public void genericConstructorParameterInInnerClass() throws Exception {
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

}
