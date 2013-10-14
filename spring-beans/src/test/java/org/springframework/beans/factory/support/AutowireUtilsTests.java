/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class AutowireUtilsTests {

	@Test
	public void genericMethodReturnTypes() {
		Method notParameterized = ReflectionUtils.findMethod(MyTypeWithMethods.class, "notParameterized", new Class[]{});
		assertEquals(String.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(notParameterized, new Object[]{}, getClass().getClassLoader()));

		Method notParameterizedWithArguments = ReflectionUtils.findMethod(MyTypeWithMethods.class, "notParameterizedWithArguments",
				new Class[] { Integer.class, Boolean.class });
		assertEquals(String.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(notParameterizedWithArguments, new Object[] { 99, true }, getClass().getClassLoader()));

		Method createProxy = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createProxy", new Class[] { Object.class });
		assertEquals(String.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createProxy, new Object[] { "foo" }, getClass().getClassLoader()));

		Method createNamedProxyWithDifferentTypes = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createNamedProxy",
				new Class[] { String.class, Object.class });
		// one argument to few
		assertNull(
				AutowireUtils.resolveReturnTypeForFactoryMethod(createNamedProxyWithDifferentTypes, new Object[]{"enigma"}, getClass().getClassLoader()));
		assertEquals(Long.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createNamedProxyWithDifferentTypes, new Object[] { "enigma", 99L }, getClass().getClassLoader()));

		Method createNamedProxyWithDuplicateTypes = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createNamedProxy",
				new Class[] { String.class, Object.class });
		assertEquals(String.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createNamedProxyWithDuplicateTypes, new Object[] { "enigma", "foo" }, getClass().getClassLoader()));

		Method createMock = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createMock", new Class[] { Class.class });
		assertEquals(Runnable.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createMock, new Object[] { Runnable.class }, getClass().getClassLoader()));
		assertEquals(Runnable.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createMock, new Object[] { Runnable.class.getName() }, getClass().getClassLoader()));

		Method createNamedMock = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createNamedMock", new Class[] { String.class,
				Class.class });
		assertEquals(Runnable.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createNamedMock, new Object[] { "foo", Runnable.class }, getClass().getClassLoader()));

		Method createVMock = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createVMock",
				new Class[] { Object.class, Class.class });
		assertEquals(Runnable.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createVMock, new Object[] { "foo", Runnable.class }, getClass().getClassLoader()));

		// Ideally we would expect String.class instead of Object.class, but
		// resolveReturnTypeForFactoryMethod() does not currently support this form of
		// look-up.
		Method extractValueFrom = ReflectionUtils.findMethod(MyTypeWithMethods.class, "extractValueFrom",
				new Class[] { MyInterfaceType.class });
		assertEquals(Object.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(extractValueFrom, new Object[] { new MySimpleInterfaceType() }, getClass().getClassLoader()));

		// Ideally we would expect Boolean.class instead of Object.class, but this
		// information is not available at run-time due to type erasure.
		Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();
		map.put(0, false);
		map.put(1, true);
		Method extractMagicValue = ReflectionUtils.findMethod(MyTypeWithMethods.class, "extractMagicValue", new Class[] { Map.class });
		assertEquals(Object.class, AutowireUtils.resolveReturnTypeForFactoryMethod(extractMagicValue, new Object[] { map }, getClass().getClassLoader()));
	}


	public interface MyInterfaceType<T> {
	}

	public class MySimpleInterfaceType implements MyInterfaceType<String> {
	}

	public static class MyTypeWithMethods<T> {

		public MyInterfaceType<Integer> integer() {
			return null;
		}

		public MySimpleInterfaceType string() {
			return null;
		}

		public Object object() {
			return null;
		}

		@SuppressWarnings("rawtypes")
		public MyInterfaceType raw() {
			return null;
		}

		public String notParameterized() {
			return null;
		}

		public String notParameterizedWithArguments(Integer x, Boolean b) {
			return null;
		}

		/**
		 * Simulates a factory method that wraps the supplied object in a proxy of the
		 * same type.
		 */
		public static <T> T createProxy(T object) {
			return null;
		}

		/**
		 * Similar to {@link #createProxy(Object)} but adds an additional argument before
		 * the argument of type {@code T}. Note that they may potentially be of the same
		 * time when invoked!
		 */
		public static <T> T createNamedProxy(String name, T object) {
			return null;
		}

		/**
		 * Simulates factory methods found in libraries such as Mockito and EasyMock.
		 */
		public static <MOCK> MOCK createMock(Class<MOCK> toMock) {
			return null;
		}

		/**
		 * Similar to {@link #createMock(Class)} but adds an additional method argument
		 * before the parameterized argument.
		 */
		public static <T> T createNamedMock(String name, Class<T> toMock) {
			return null;
		}

		/**
		 * Similar to {@link #createNamedMock(String, Class)} but adds an additional
		 * parameterized type.
		 */
		public static <V extends Object, T> T createVMock(V name, Class<T> toMock) {
			return null;
		}

		/**
		 * Extract some value of the type supported by the interface (i.e., by a concrete,
		 * non-generic implementation of the interface).
		 */
		public static <T> T extractValueFrom(MyInterfaceType<T> myInterfaceType) {
			return null;
		}

		/**
		 * Extract some magic value from the supplied map.
		 */
		public static <K, V> V extractMagicValue(Map<K, V> map) {
			return null;
		}

		public void readIntegerInputMessage(MyInterfaceType<Integer> message) {
		}

		public void readIntegerArrayInputMessage(MyInterfaceType<Integer>[] message) {
		}

		public void readGenericArrayInputMessage(T[] message) {
		}
	}

}
