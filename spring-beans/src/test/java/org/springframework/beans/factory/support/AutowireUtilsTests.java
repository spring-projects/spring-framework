/*
 * Copyright 2002-2018 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.util.ReflectionUtils;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class AutowireUtilsTests {

	@Test
	public void genericMethodReturnTypes() {
		Method notParameterized = ReflectionUtils.findMethod(MyTypeWithMethods.class, "notParameterized");
		assertEquals(String.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(notParameterized, new Object[]{}, getClass().getClassLoader()));

		Method notParameterizedWithArguments = ReflectionUtils.findMethod(MyTypeWithMethods.class, "notParameterizedWithArguments", Integer.class, Boolean.class);
		assertEquals(String.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(notParameterizedWithArguments, new Object[]{99, true}, getClass().getClassLoader()));

		Method createProxy = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createProxy", Object.class);
		assertEquals(String.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createProxy, new Object[]{"foo"}, getClass().getClassLoader()));

		Method createNamedProxyWithDifferentTypes = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createNamedProxy", String.class, Object.class);
		assertEquals(Long.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createNamedProxyWithDifferentTypes, new Object[]{"enigma", 99L}, getClass().getClassLoader()));

		Method createNamedProxyWithDuplicateTypes = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createNamedProxy", String.class, Object.class);
		assertEquals(String.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createNamedProxyWithDuplicateTypes, new Object[]{"enigma", "foo"}, getClass().getClassLoader()));

		Method createMock = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createMock", Class.class);
		assertEquals(Runnable.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createMock, new Object[]{Runnable.class}, getClass().getClassLoader()));
		assertEquals(Runnable.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createMock, new Object[]{Runnable.class.getName()}, getClass().getClassLoader()));

		Method createNamedMock = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createNamedMock", String.class, Class.class);
		assertEquals(Runnable.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createNamedMock, new Object[]{"foo", Runnable.class}, getClass().getClassLoader()));

		Method createVMock = ReflectionUtils.findMethod(MyTypeWithMethods.class, "createVMock", Object.class, Class.class);
		assertEquals(Runnable.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(createVMock, new Object[]{"foo", Runnable.class}, getClass().getClassLoader()));

		// Ideally we would expect String.class instead of Object.class, but
		// resolveReturnTypeForFactoryMethod() does not currently support this form of
		// look-up.
		Method extractValueFrom = ReflectionUtils.findMethod(MyTypeWithMethods.class, "extractValueFrom", MyInterfaceType.class);
		assertEquals(Object.class,
				AutowireUtils.resolveReturnTypeForFactoryMethod(extractValueFrom, new Object[]{new MySimpleInterfaceType()}, getClass().getClassLoader()));

		// Ideally we would expect Boolean.class instead of Object.class, but this
		// information is not available at run-time due to type erasure.
		Map<Integer, Boolean> map = new HashMap<>();
		map.put(0, false);
		map.put(1, true);
		Method extractMagicValue = ReflectionUtils.findMethod(MyTypeWithMethods.class, "extractMagicValue", Map.class);
		assertEquals(Object.class, AutowireUtils.resolveReturnTypeForFactoryMethod(extractMagicValue, new Object[]{map}, getClass().getClassLoader()));
	}

	@Test
	public void marked_parameters_are_candidate_for_autowiring() throws NoSuchMethodException {
		Constructor<AutowirableClass> autowirableConstructor = ReflectionUtils.accessibleConstructor(
				AutowirableClass.class, String.class, String.class, String.class, String.class);

		for (int parameterIndex = 0; parameterIndex < autowirableConstructor.getParameterCount(); parameterIndex++) {
			Parameter parameter = autowirableConstructor.getParameters()[parameterIndex];
			assertTrue("Parameter " + parameter + " must be autowirable", AutowireUtils.isAutowirable(parameter, parameterIndex));
		}
	}

	@Test
	public void not_marked_parameters_are_not_candidate_for_autowiring() throws NoSuchMethodException {
		Constructor<AutowirableClass> notAutowirableConstructor = ReflectionUtils.accessibleConstructor(AutowirableClass.class, String.class);

		for (int parameterIndex = 0; parameterIndex < notAutowirableConstructor.getParameterCount(); parameterIndex++) {
			Parameter parameter = notAutowirableConstructor.getParameters()[parameterIndex];
			assertFalse("Parameter " + parameter + " must not be autowirable", AutowireUtils.isAutowirable(parameter, 0));
		}
	}

	@Test
	public void dependency_resolution_for_marked_parameters() throws NoSuchMethodException {
		Constructor<AutowirableClass> autowirableConstructor = ReflectionUtils.accessibleConstructor(
				AutowirableClass.class, String.class, String.class, String.class, String.class);
		AutowireCapableBeanFactory beanFactory = Mockito.mock(AutowireCapableBeanFactory.class);
		// BeanFactory will return the DependencyDescriptor for convenience and to avoid using an ArgumentCaptor
		when(beanFactory.resolveDependency(any(), isNull())).thenAnswer(iom -> iom.getArgument(0));

		for (int parameterIndex = 0; parameterIndex < autowirableConstructor.getParameterCount(); parameterIndex++) {
			Parameter parameter = autowirableConstructor.getParameters()[parameterIndex];
			DependencyDescriptor intermediateDependencyDescriptor = (DependencyDescriptor) AutowireUtils.resolveDependency(
					parameter, parameterIndex, AutowirableClass.class, beanFactory);
			assertEquals(intermediateDependencyDescriptor.getAnnotatedElement(), autowirableConstructor);
			assertEquals(intermediateDependencyDescriptor.getMethodParameter().getParameter(), parameter);
		}
	}

	public interface MyInterfaceType<T> {
	}

	public static class MyTypeWithMethods<T> {

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

		public void readIntegerInputMessage(MyInterfaceType<Integer> message) {
		}

		public void readIntegerArrayInputMessage(MyInterfaceType<Integer>[] message) {
		}

		public void readGenericArrayInputMessage(T[] message) {
		}
	}

	public static class AutowirableClass {
		public AutowirableClass(@Autowired String firstParameter,
								@Qualifier("someQualifier") String secondParameter,
								@Value("${someValue}") String thirdParameter,
								@Autowired(required = false) String fourthParameter) {
		}

		public AutowirableClass(String notAutowirableParameter) {
		}
	}

	public class MySimpleInterfaceType implements MyInterfaceType<String> {
	}
}
