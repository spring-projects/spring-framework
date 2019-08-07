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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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
		assertEquals(stringParameter, stringParameter);
		assertEquals(longParameter, longParameter);
		assertEquals(intReturnType, intReturnType);

		assertFalse(stringParameter.equals(longParameter));
		assertFalse(stringParameter.equals(intReturnType));
		assertFalse(longParameter.equals(stringParameter));
		assertFalse(longParameter.equals(intReturnType));
		assertFalse(intReturnType.equals(stringParameter));
		assertFalse(intReturnType.equals(longParameter));

		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		MethodParameter methodParameter = new MethodParameter(method, 0);
		assertEquals(stringParameter, methodParameter);
		assertEquals(methodParameter, stringParameter);
		assertNotEquals(longParameter, methodParameter);
		assertNotEquals(methodParameter, longParameter);
	}

	@Test
	public void testHashCode() throws NoSuchMethodException {
		assertEquals(stringParameter.hashCode(), stringParameter.hashCode());
		assertEquals(longParameter.hashCode(), longParameter.hashCode());
		assertEquals(intReturnType.hashCode(), intReturnType.hashCode());

		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		MethodParameter methodParameter = new MethodParameter(method, 0);
		assertEquals(stringParameter.hashCode(), methodParameter.hashCode());
		assertNotEquals(longParameter.hashCode(), methodParameter.hashCode());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testFactoryMethods() {
		assertEquals(stringParameter, MethodParameter.forMethodOrConstructor(method, 0));
		assertEquals(longParameter, MethodParameter.forMethodOrConstructor(method, 1));

		assertEquals(stringParameter, MethodParameter.forExecutable(method, 0));
		assertEquals(longParameter, MethodParameter.forExecutable(method, 1));

		assertEquals(stringParameter, MethodParameter.forParameter(method.getParameters()[0]));
		assertEquals(longParameter, MethodParameter.forParameter(method.getParameters()[1]));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIndexValidation() {
		new MethodParameter(method, 2);
	}

	@Test
	public void annotatedConstructorParameterInStaticNestedClass() throws Exception {
		Constructor<?> constructor = NestedClass.class.getDeclaredConstructor(String.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(constructor, 0);
		assertEquals(String.class, methodParameter.getParameterType());
		assertNotNull("Failed to find @Param annotation", methodParameter.getParameterAnnotation(Param.class));
	}

	@Test  // SPR-16652
	public void annotatedConstructorParameterInInnerClass() throws Exception {
		Constructor<?> constructor = InnerClass.class.getConstructor(getClass(), String.class, Callable.class);

		MethodParameter methodParameter = MethodParameter.forExecutable(constructor, 0);
		assertEquals(getClass(), methodParameter.getParameterType());
		assertNull(methodParameter.getParameterAnnotation(Param.class));

		methodParameter = MethodParameter.forExecutable(constructor, 1);
		assertEquals(String.class, methodParameter.getParameterType());
		assertNotNull("Failed to find @Param annotation", methodParameter.getParameterAnnotation(Param.class));

		methodParameter = MethodParameter.forExecutable(constructor, 2);
		assertEquals(Callable.class, methodParameter.getParameterType());
		assertNull(methodParameter.getParameterAnnotation(Param.class));
	}

	@Test  // SPR-16734
	public void genericConstructorParameterInInnerClass() throws Exception {
		Constructor<?> constructor = InnerClass.class.getConstructor(getClass(), String.class, Callable.class);

		MethodParameter methodParameter = MethodParameter.forExecutable(constructor, 0);
		assertEquals(getClass(), methodParameter.getParameterType());
		assertEquals(getClass(), methodParameter.getGenericParameterType());

		methodParameter = MethodParameter.forExecutable(constructor, 1);
		assertEquals(String.class, methodParameter.getParameterType());
		assertEquals(String.class, methodParameter.getGenericParameterType());

		methodParameter = MethodParameter.forExecutable(constructor, 2);
		assertEquals(Callable.class, methodParameter.getParameterType());
		assertEquals(ResolvableType.forClassWithGenerics(Callable.class, Integer.class).getType(),
				methodParameter.getGenericParameterType());
	}

	@Test
	public void multipleResolveParameterTypeCalls() throws Exception {
		Method method = ArrayList.class.getMethod("get", int.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(method, -1);
		assertEquals(Object.class, methodParameter.getParameterType());
		GenericTypeResolver.resolveParameterType(methodParameter, StringList.class);
		assertEquals(String.class, methodParameter.getParameterType());
		GenericTypeResolver.resolveParameterType(methodParameter, IntegerList.class);
		assertEquals(Integer.class, methodParameter.getParameterType());
	}

	@Test
	public void equalsAndHashCodeConsidersContainingClass() throws Exception {
		Method method = ArrayList.class.getMethod("get", int.class);
		MethodParameter m1 = MethodParameter.forExecutable(method, -1);
		MethodParameter m2 = MethodParameter.forExecutable(method, -1);
		MethodParameter m3 = MethodParameter.forExecutable(method, -1).nested();
		assertEquals(m1, m2);
		assertNotEquals(m1, m3);
		assertEquals(m1.hashCode(), m2.hashCode());
	}

	@Test
	public void equalsAndHashCodeConsidersNesting() throws Exception {
		Method method = ArrayList.class.getMethod("get", int.class);
		MethodParameter m1 = MethodParameter.forExecutable(method, -1);
		GenericTypeResolver.resolveParameterType(m1, StringList.class);
		MethodParameter m2 = MethodParameter.forExecutable(method, -1);
		GenericTypeResolver.resolveParameterType(m2, StringList.class);
		MethodParameter m3 = MethodParameter.forExecutable(method, -1);
		GenericTypeResolver.resolveParameterType(m3, IntegerList.class);
		MethodParameter m4 = MethodParameter.forExecutable(method, -1);
		assertEquals(m1, m2);
		assertNotEquals(m1, m3);
		assertNotEquals(m1, m4);
		assertEquals(m1.hashCode(), m2.hashCode());
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
