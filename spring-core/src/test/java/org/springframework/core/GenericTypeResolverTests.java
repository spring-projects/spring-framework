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
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
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

	@Test
	public void testGetTypeVariableMap() throws Exception {
		Map<TypeVariable, Type> map;

		map = GenericTypeResolver.getTypeVariableMap(MySimpleInterfaceType.class);
		assertThat(map.toString(), equalTo("{T=class java.lang.String}"));

		map = GenericTypeResolver.getTypeVariableMap(MyCollectionInterfaceType.class);
		assertThat(map.toString(), equalTo("{T=java.util.Collection<java.lang.String>}"));

		map = GenericTypeResolver.getTypeVariableMap(MyCollectionSuperclassType.class);
		assertThat(map.toString(), equalTo("{T=java.util.Collection<java.lang.String>}"));

		map = GenericTypeResolver.getTypeVariableMap(MySimpleTypeWithMethods.class);
		assertThat(map.toString(), equalTo("{T=class java.lang.Integer}"));

		map = GenericTypeResolver.getTypeVariableMap(TopLevelClass.class);
		assertThat(map.toString(), equalTo("{}"));

		map = GenericTypeResolver.getTypeVariableMap(TypedTopLevelClass.class);
		assertThat(map.toString(), equalTo("{T=class java.lang.Integer}"));

		map = GenericTypeResolver.getTypeVariableMap(TypedTopLevelClass.TypedNested.class);
		assertThat(map.size(), equalTo(2));
		Type t = null;
		Type x = null;
		for (Map.Entry<TypeVariable, Type> entry : map.entrySet()) {
			if(entry.getKey().toString().equals("T")) {
				t = entry.getValue();
			}
			else {
				x = entry.getValue();
			}
		}
		assertThat(t, equalTo((Type) Integer.class));
		assertThat(x, equalTo((Type) Long.class));
	}

	@Test
	public void getGenericsCannotBeResolved() throws Exception {
		// SPR-11030
		Class<?>[] resolved = GenericTypeResolver.resolveTypeArguments(List.class, Iterable.class);
		assertNull(resolved);
	}

	@Test
	public void getRawMapTypeCannotBeResolved() throws Exception {
		// SPR-11052
		Class<?>[] resolved = GenericTypeResolver.resolveTypeArguments(Map.class, Map.class);
		assertNull(resolved);
	}

	@Test
	public void getGenericsOnArrayFromParamCannotBeResolved() throws Exception {
		// SPR-11044
		MethodParameter methodParameter = MethodParameter.forMethodOrConstructor(
				WithArrayBase.class.getDeclaredMethod("array", Object[].class), 0);
		Class<?> resolved = GenericTypeResolver.resolveParameterType(methodParameter, WithArray.class);
		assertThat(resolved, equalTo((Class) Object[].class));
	}

	@Test
	public void getGenericsOnArrayFromReturnCannotBeResolved() throws Exception {
		// SPR-11044
		Class<?> resolved = GenericTypeResolver.resolveReturnType(
				WithArrayBase.class.getDeclaredMethod("array", Object[].class),
				WithArray.class);
		assertThat(resolved, equalTo((Class) Object[].class));
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

	static class TopLevelClass<T> {
		class Nested<X> {
		}
	}

	static class TypedTopLevelClass extends TopLevelClass<Integer> {
		class TypedNested extends Nested<Long> {
		}
	}

	static abstract class WithArrayBase<T> {

		public abstract T[] array(T... args);
	}

	static abstract class WithArray<T> extends WithArrayBase<T> {
	}

}
