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

package org.springframework.core;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.GenericTypeResolver.getTypeVariableMap;
import static org.springframework.core.GenericTypeResolver.resolveReturnTypeArgument;
import static org.springframework.core.GenericTypeResolver.resolveType;
import static org.springframework.core.GenericTypeResolver.resolveTypeArgument;
import static org.springframework.util.ReflectionUtils.findMethod;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Yanming Zhou
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class GenericTypeResolverTests {

	@Test
	void simpleInterfaceType() {
		assertThat(resolveTypeArgument(MySimpleInterfaceType.class, MyInterfaceType.class)).isEqualTo(String.class);
	}

	@Test
	void simpleCollectionInterfaceType() {
		assertThat(resolveTypeArgument(MyCollectionInterfaceType.class, MyInterfaceType.class)).isEqualTo(Collection.class);
	}

	@Test
	void simpleSuperclassType() {
		assertThat(resolveTypeArgument(MySimpleSuperclassType.class, MySuperclassType.class)).isEqualTo(String.class);
	}

	@Test
	void simpleCollectionSuperclassType() {
		assertThat(resolveTypeArgument(MyCollectionSuperclassType.class, MySuperclassType.class)).isEqualTo(Collection.class);
	}

	@Test
	void nullIfNotResolvable() {
		GenericClass<String> obj = new GenericClass<>();
		assertThat(resolveTypeArgument(obj.getClass(), GenericClass.class)).isNull();
	}

	@Test
	void methodReturnTypes() {
		assertThat(resolveReturnTypeArgument(method(MyTypeWithMethods.class, "integer"), MyInterfaceType.class)).isEqualTo(Integer.class);
		assertThat(resolveReturnTypeArgument(method(MyTypeWithMethods.class, "string"), MyInterfaceType.class)).isEqualTo(String.class);
		assertThat(resolveReturnTypeArgument(method(MyTypeWithMethods.class, "character"), MyAbstractType.class)).isEqualTo(Character.class);
		assertThat(resolveReturnTypeArgument(method(MyTypeWithMethods.class, "raw"), MyInterfaceType.class)).isNull();
		assertThat(resolveReturnTypeArgument(method(MyTypeWithMethods.class, "object"), MyInterfaceType.class)).isNull();
	}

	@Test
	void testResolveType() {
		Method intMessageMethod = method(MyTypeWithMethods.class, "readIntegerInputMessage", MyInterfaceType.class);
		MethodParameter intMessageMethodParam = new MethodParameter(intMessageMethod, 0);
		assertThat(resolveType(intMessageMethodParam.getGenericParameterType(), new HashMap<>())).isEqualTo(MyInterfaceType.class);

		Method intArrMessageMethod = method(MyTypeWithMethods.class, "readIntegerArrayInputMessage",
				MyInterfaceType[].class);
		MethodParameter intArrMessageMethodParam = new MethodParameter(intArrMessageMethod, 0);
		assertThat(resolveType(intArrMessageMethodParam.getGenericParameterType(), new HashMap<>())).isEqualTo(MyInterfaceType[].class);

		Method genericArrMessageMethod = method(MySimpleTypeWithMethods.class, "readGenericArrayInputMessage",
				Object[].class);
		MethodParameter genericArrMessageMethodParam = new MethodParameter(genericArrMessageMethod, 0);
		Map<TypeVariable, Type> varMap = getTypeVariableMap(MySimpleTypeWithMethods.class);
		assertThat(resolveType(genericArrMessageMethodParam.getGenericParameterType(), varMap)).isEqualTo(Integer[].class);
	}

	@Test
	void boundParameterizedType() {
		assertThat(resolveTypeArgument(TestImpl.class, TestIfc.class)).isEqualTo(B.class);
	}

	@Test
	void testGetTypeVariableMap() {
		Map<TypeVariable, Type> map;

		map = GenericTypeResolver.getTypeVariableMap(MySimpleInterfaceType.class);
		assertThat(map.toString()).isEqualTo("{T=class java.lang.String}");

		map = GenericTypeResolver.getTypeVariableMap(MyCollectionInterfaceType.class);
		assertThat(map.toString()).isEqualTo("{T=java.util.Collection<java.lang.String>}");

		map = GenericTypeResolver.getTypeVariableMap(MyCollectionSuperclassType.class);
		assertThat(map.toString()).isEqualTo("{T=java.util.Collection<java.lang.String>}");

		map = GenericTypeResolver.getTypeVariableMap(MySimpleTypeWithMethods.class);
		assertThat(map.toString()).isEqualTo("{T=class java.lang.Integer}");

		map = GenericTypeResolver.getTypeVariableMap(TopLevelClass.class);
		assertThat(map.toString()).isEqualTo("{}");

		map = GenericTypeResolver.getTypeVariableMap(TypedTopLevelClass.class);
		assertThat(map.toString()).isEqualTo("{T=class java.lang.Integer}");

		map = GenericTypeResolver.getTypeVariableMap(TypedTopLevelClass.TypedNested.class);
		assertThat(map).hasSize(2);
		Type t = null;
		Type x = null;
		for (Map.Entry<TypeVariable, Type> entry : map.entrySet()) {
			if (entry.getKey().toString().equals("T")) {
				t = entry.getValue();
			}
			else {
				x = entry.getValue();
			}
		}
		assertThat(t).isEqualTo(Integer.class);
		assertThat(x).isEqualTo(Long.class);
	}

	@Test
	void resolveTypeArgumentsOfAbstractType() {
		Class<?>[] resolved = GenericTypeResolver.resolveTypeArguments(MyConcreteType.class, MyAbstractType.class);
		assertThat(resolved).containsExactly(Character.class);
	}

	@Test  // SPR-11030
	void getGenericsCannotBeResolved() {
		Class<?>[] resolved = GenericTypeResolver.resolveTypeArguments(List.class, Iterable.class);
		assertThat(resolved).isNull();
	}

	@Test  // SPR-11052
	void getRawMapTypeCannotBeResolved() {
		Class<?>[] resolved = GenericTypeResolver.resolveTypeArguments(Map.class, Map.class);
		assertThat(resolved).isNull();
	}

	@Test  // SPR-11044
	@SuppressWarnings("deprecation")
	void getGenericsOnArrayFromParamCannotBeResolved() throws Exception {
		MethodParameter methodParameter = MethodParameter.forExecutable(
				WithArrayBase.class.getDeclaredMethod("array", Object[].class), 0);
		Class<?> resolved = GenericTypeResolver.resolveParameterType(methodParameter, WithArray.class);
		assertThat(resolved).isEqualTo(Object[].class);
	}

	@Test  // SPR-11044
	void getGenericsOnArrayFromReturnCannotBeResolved() throws Exception {
		Class<?> resolved = GenericTypeResolver.resolveReturnType(
				WithArrayBase.class.getDeclaredMethod("array", Object[].class), WithArray.class);
		assertThat(resolved).isEqualTo(Object[].class);
	}

	@Test  // SPR-11763
	void resolveIncompleteTypeVariables() {
		Class<?>[] resolved = GenericTypeResolver.resolveTypeArguments(IdFixingRepository.class, Repository.class);
		assertThat(resolved).isNotNull();
		assertThat(resolved).hasSize(2);
		assertThat(resolved[0]).isEqualTo(Object.class);
		assertThat(resolved[1]).isEqualTo(Long.class);
	}

	@Test
	void resolvePartiallySpecializedTypeVariables() {
		Type resolved = resolveType(BiGenericClass.class.getTypeParameters()[0], TypeFixedBiGenericClass.class);
		assertThat(resolved).isEqualTo(D.class);
	}

	@Test
	void resolveTransitiveTypeVariableWithDifferentName() {
		Type resolved = resolveType(BiGenericClass.class.getTypeParameters()[1], TypeFixedBiGenericClass.class);
		assertThat(resolved).isEqualTo(E.class);
	}

	@Test
	void resolveMethodParameterWithNestedGenerics() {
		Method method = method(WithMethodParameter.class, "nestedGenerics", List.class);
		MethodParameter methodParameter = new MethodParameter(method, 0);
		Type resolvedType = resolveType(methodParameter.getGenericParameterType(), WithMethodParameter.class);
		ParameterizedTypeReference<List<Map<String, Integer>>> reference = new ParameterizedTypeReference<>() {};
		assertThat(resolvedType).isEqualTo(reference.getType());
	}

	@Test
	void resolveNestedTypeVariable() throws Exception {
		Type resolved = resolveType(ListOfListSupplier.class.getMethod("get").getGenericReturnType(),
				StringListOfListSupplier.class);
		assertThat(ResolvableType.forType(resolved).getGeneric(0).getGeneric(0).resolve()).isEqualTo(String.class);
	}

	private static Method method(Class<?> target, String methodName, Class<?>... parameterTypes) {
		Method method = findMethod(target, methodName, parameterTypes);
		assertThat(method).describedAs(target.getName() + "#" + methodName).isNotNull();
		return method;
	}

	public interface MyInterfaceType<T> {
	}

	public class MySimpleInterfaceType implements MyInterfaceType<String> {
	}

	public class MyCollectionInterfaceType implements MyInterfaceType<Collection<String>> {
	}

	public abstract class MyAbstractType<T> implements MyInterfaceType<T> {
	}

	public class MyConcreteType extends MyAbstractType<Character> {
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

		public MyConcreteType character() { return null; }

		public Object object() {
			return null;
		}

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

	class C extends A {}

	class D extends B<Long> {}

	class E extends C {}

	class TestIfc<T>{}

	class TestImpl<I extends A, T extends B<I>> extends TestIfc<T>{
	}

	abstract static class BiGenericClass<T extends B<?>, V extends A> {}

	abstract static class SpecializedBiGenericClass<U extends C> extends BiGenericClass<D, U>{}

	static class TypeFixedBiGenericClass extends SpecializedBiGenericClass<E> {}

	static class TopLevelClass<T> {
		class Nested<X> {
		}
	}

	static class TypedTopLevelClass extends TopLevelClass<Integer> {
		class TypedNested extends Nested<Long> {
		}
	}

	abstract static class WithArrayBase<T> {

		public abstract T[] array(T... args);
	}

	abstract static class WithArray<T> extends WithArrayBase<T> {
	}

	interface Repository<T, ID extends Serializable> {
	}

	interface IdFixingRepository<T> extends Repository<T, Long> {
	}

	static class WithMethodParameter {
		public void nestedGenerics(List<Map<String, Integer>> input) {
		}
	}

	public interface ListOfListSupplier<T> {

		List<List<T>> get();
	}

	public interface StringListOfListSupplier extends ListOfListSupplier<String> {
	}

}
