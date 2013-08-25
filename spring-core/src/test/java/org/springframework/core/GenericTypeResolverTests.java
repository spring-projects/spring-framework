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

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.springframework.core.GenericTypeResolver.*;
import static org.springframework.util.ReflectionUtils.*;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class GenericTypeResolverTests {

	@Test
	public void simpleInterfaceType() {
		assertEquals(String.class, resolveTypeArgument(MySimpleInterfaceType.class, MyInterfaceType.class));
	}

	@Test
	public void simpleCollectionInterfaceType() {
		assertEquals(Collection.class, resolveTypeArgument(MyCollectionInterfaceType.class, MyInterfaceType.class));
	}

	@Test
	public void simpleSuperclassType() {
		assertEquals(String.class, resolveTypeArgument(MySimpleSuperclassType.class, MySuperclassType.class));
	}

	@Test
	public void simpleCollectionSuperclassType() {
		assertEquals(Collection.class, resolveTypeArgument(MyCollectionSuperclassType.class, MySuperclassType.class));
	}

	@Test
	public void nullIfNotResolvable() {
		GenericClass<String> obj = new GenericClass<String>();
		assertNull(resolveTypeArgument(obj.getClass(), GenericClass.class));
	}

	@Test
	public void methodReturnTypes() {
		assertEquals(Integer.class,
			resolveReturnTypeArgument(findMethod(MyTypeWithMethods.class, "integer"), MyInterfaceType.class));
		assertEquals(String.class,
			resolveReturnTypeArgument(findMethod(MyTypeWithMethods.class, "string"), MyInterfaceType.class));
		assertEquals(null, resolveReturnTypeArgument(findMethod(MyTypeWithMethods.class, "raw"), MyInterfaceType.class));
		assertEquals(null,
			resolveReturnTypeArgument(findMethod(MyTypeWithMethods.class, "object"), MyInterfaceType.class));
	}

	/**
	 * @since 3.2
	 */
	@Test
	public void genericMethodReturnTypes() {
		Method notParameterized = findMethod(MyTypeWithMethods.class, "notParameterized", new Class[] {});
		assertEquals(String.class, resolveReturnTypeForGenericMethod(notParameterized, new Object[] {}));

		Method notParameterizedWithArguments = findMethod(MyTypeWithMethods.class, "notParameterizedWithArguments",
			new Class[] { Integer.class, Boolean.class });
		assertEquals(String.class,
			resolveReturnTypeForGenericMethod(notParameterizedWithArguments, new Object[] { 99, true }));

		Method createProxy = findMethod(MyTypeWithMethods.class, "createProxy", new Class[] { Object.class });
		assertEquals(String.class, resolveReturnTypeForGenericMethod(createProxy, new Object[] { "foo" }));

		Method createNamedProxyWithDifferentTypes = findMethod(MyTypeWithMethods.class, "createNamedProxy",
			new Class[] { String.class, Object.class });
		// one argument to few
		assertNull(resolveReturnTypeForGenericMethod(createNamedProxyWithDifferentTypes, new Object[] { "enigma" }));
		assertEquals(Long.class,
			resolveReturnTypeForGenericMethod(createNamedProxyWithDifferentTypes, new Object[] { "enigma", 99L }));

		Method createNamedProxyWithDuplicateTypes = findMethod(MyTypeWithMethods.class, "createNamedProxy",
			new Class[] { String.class, Object.class });
		assertEquals(String.class,
			resolveReturnTypeForGenericMethod(createNamedProxyWithDuplicateTypes, new Object[] { "enigma", "foo" }));

		Method createMock = findMethod(MyTypeWithMethods.class, "createMock", new Class[] { Class.class });
		assertEquals(Runnable.class, resolveReturnTypeForGenericMethod(createMock, new Object[] { Runnable.class }));

		Method createNamedMock = findMethod(MyTypeWithMethods.class, "createNamedMock", new Class[] { String.class,
			Class.class });
		assertEquals(Runnable.class,
			resolveReturnTypeForGenericMethod(createNamedMock, new Object[] { "foo", Runnable.class }));

		Method createVMock = findMethod(MyTypeWithMethods.class, "createVMock",
			new Class[] { Object.class, Class.class });
		assertEquals(Runnable.class,
			resolveReturnTypeForGenericMethod(createVMock, new Object[] { "foo", Runnable.class }));

		// Ideally we would expect String.class instead of Object.class, but
		// resolveReturnTypeForGenericMethod() does not currently support this form of
		// look-up.
		Method extractValueFrom = findMethod(MyTypeWithMethods.class, "extractValueFrom",
			new Class[] { MyInterfaceType.class });
		assertEquals(Object.class,
			resolveReturnTypeForGenericMethod(extractValueFrom, new Object[] { new MySimpleInterfaceType() }));

		// Ideally we would expect Boolean.class instead of Object.class, but this
		// information is not available at run-time due to type erasure.
		Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();
		map.put(0, false);
		map.put(1, true);
		Method extractMagicValue = findMethod(MyTypeWithMethods.class, "extractMagicValue", new Class[] { Map.class });
		assertEquals(Object.class, resolveReturnTypeForGenericMethod(extractMagicValue, new Object[] { map }));
	}

	/**
	 * @since 3.2
	 */
	@Test
	public void testResolveType() {
		Method intMessageMethod = findMethod(MyTypeWithMethods.class, "readIntegerInputMessage", MyInterfaceType.class);
		MethodParameter intMessageMethodParam = new MethodParameter(intMessageMethod, 0);
		assertEquals(MyInterfaceType.class,
			resolveType(intMessageMethodParam.getGenericParameterType(), new HashMap<TypeVariable, Type>()));

		Method intArrMessageMethod = findMethod(MyTypeWithMethods.class, "readIntegerArrayInputMessage",
			MyInterfaceType[].class);
		MethodParameter intArrMessageMethodParam = new MethodParameter(intArrMessageMethod, 0);
		assertEquals(MyInterfaceType[].class,
			resolveType(intArrMessageMethodParam.getGenericParameterType(), new HashMap<TypeVariable, Type>()));

		Method genericArrMessageMethod = findMethod(MySimpleTypeWithMethods.class, "readGenericArrayInputMessage",
			Object[].class);
		MethodParameter genericArrMessageMethodParam = new MethodParameter(genericArrMessageMethod, 0);
		Map<TypeVariable, Type> varMap = getTypeVariableMap(MySimpleTypeWithMethods.class);
		assertEquals(Integer[].class, resolveType(genericArrMessageMethodParam.getGenericParameterType(), varMap));
	}

	@Test
	public void testBoundParameterizedType() {
		assertEquals(B.class, resolveTypeArgument(TestImpl.class, ITest.class));
	}


	public interface MyInterfaceType<T> {
	}

	public class MySimpleInterfaceType implements MyInterfaceType<String> {
	}

	public class MyCollectionInterfaceType implements MyInterfaceType<Collection<String>> {
	}

	public abstract class MySuperclassType<T> {
	}

	public class MySimpleSuperclassType extends MySuperclassType<String> {
	}

	public class MyCollectionSuperclassType extends MySuperclassType<Collection<String>> {
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

	public static class MySimpleTypeWithMethods extends MyTypeWithMethods<Integer> {
	}

	static class GenericClass<T> {
	}

	class A{}

	class B<T>{}

	class ITest<T>{}

	class TestImpl<I extends A, T extends B<I>> extends ITest<T>{
	}

}
