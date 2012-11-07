/*
 * Copyright 2011-2012 the original author or authors.
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
package org.springframework.beans.type;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.beans.type.ClassTypeInformation.*;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for {@link ClassTypeInformation}.
 * 
 * @author Oliver Gierke
 */
public class ClassTypeInformationUnitTests {

	@Test
	public void discoversTypeForSimpleGenericField() {

		TypeInformation<ConcreteType> discoverer = ClassTypeInformation.from(ConcreteType.class);
		assertEquals(ConcreteType.class, discoverer.getType());
		TypeInformation<?> content = discoverer.getProperty("content");
		assertEquals(String.class, content.getType());
		assertNull(content.getComponentType());
		assertNull(content.getMapValueType());
	}

	@Test
	public void discoversTypeForNestedGenericField() {

		TypeInformation<ConcreteWrapper> discoverer = ClassTypeInformation.from(ConcreteWrapper.class);
		assertEquals(ConcreteWrapper.class, discoverer.getType());
		TypeInformation<?> wrapper = discoverer.getProperty("wrapped");
		assertEquals(GenericType.class, wrapper.getType());
		TypeInformation<?> content = wrapper.getProperty("content");

		assertEquals(String.class, content.getType());
		assertEquals(String.class, discoverer.getProperty("wrapped").getProperty("content").getType());
		assertEquals(String.class, discoverer.getProperty("wrapped.content").getType());
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void discoversBoundType() {

		TypeInformation<GenericTypeWithBound> information = ClassTypeInformation.from(GenericTypeWithBound.class);
		assertEquals(Person.class, information.getProperty("person").getType());
	}

	@Test
	public void discoversBoundTypeForSpecialization() {

		TypeInformation<SpecialGenericTypeWithBound> information = ClassTypeInformation
				.from(SpecialGenericTypeWithBound.class);
		assertEquals(SpecialPerson.class, information.getProperty("person").getType());
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void discoversBoundTypeForNested() {

		TypeInformation<AnotherGenericType> information = ClassTypeInformation.from(AnotherGenericType.class);
		assertEquals(GenericTypeWithBound.class, information.getProperty("nested").getType());
		assertEquals(Person.class, information.getProperty("nested.person").getType());
	}

	@Test
	public void discoversArraysAndCollections() {
		TypeInformation<StringCollectionContainer> information = ClassTypeInformation.from(StringCollectionContainer.class);

		TypeInformation<?> property = information.getProperty("array");
		assertEquals(property.getComponentType().getType(), String.class);

		Class<?> type = property.getType();
		assertEquals(String[].class, type);
		assertThat(type.isArray(), is(true));

		property = information.getProperty("foo");
		assertEquals(Collection[].class, property.getType());
		assertEquals(Collection.class, property.getComponentType().getType());
		assertEquals(String.class, property.getComponentType().getComponentType().getType());

		property = information.getProperty("rawSet");
		assertEquals(Set.class, property.getType());
		assertEquals(Object.class, property.getComponentType().getType());
		assertNull(property.getMapValueType());
	}

	@Test
	public void discoversMapValueType() {

		TypeInformation<StringMapContainer> information = ClassTypeInformation.from(StringMapContainer.class);
		TypeInformation<?> genericMap = information.getProperty("genericMap");
		assertEquals(Map.class, genericMap.getType());
		assertEquals(String.class, genericMap.getMapValueType().getType());

		TypeInformation<?> map = information.getProperty("map");
		assertEquals(Map.class, map.getType());
		assertEquals(Calendar.class, map.getMapValueType().getType());
	}

	@Test
	public void typeInfoDoesNotEqualForGenericTypesWithDifferentParent() {

		TypeInformation<ConcreteWrapper> first = ClassTypeInformation.from(ConcreteWrapper.class);
		TypeInformation<AnotherConcreteWrapper> second = ClassTypeInformation.from(AnotherConcreteWrapper.class);

		assertFalse(first.getProperty("wrapped").equals(second.getProperty("wrapped")));
	}

	@Test
	public void handlesPropertyFieldMismatchCorrectly() {

		TypeInformation<PropertyGetter> from = ClassTypeInformation.from(PropertyGetter.class);

		TypeInformation<?> property = from.getProperty("_name");
		assertThat(property, is(notNullValue()));
		assertThat(property.getType(), is(typeCompatibleWith(String.class)));

		property = from.getProperty("name");
		assertThat(property, is(notNullValue()));
		assertThat(property.getType(), is(typeCompatibleWith(byte[].class)));
	}

	/**
	 * @see DATACMNS-77
	 */
	@Test
	public void returnsSameInstanceForCachedClass() {

		TypeInformation<PropertyGetter> info = ClassTypeInformation.from(PropertyGetter.class);
		assertThat(ClassTypeInformation.from(PropertyGetter.class), is(sameInstance(info)));
	}

	/**
	 * @see DATACMNS-39
	 */
	@Test
	public void resolvesWildCardTypeCorrectly() {

		TypeInformation<ClassWithWildCardBound> information = ClassTypeInformation.from(ClassWithWildCardBound.class);

		TypeInformation<?> property = information.getProperty("wildcard");
		assertThat(property.isCollectionLike(), is(true));
		assertThat(property.getComponentType().getType(), is(typeCompatibleWith(String.class)));

		property = information.getProperty("complexWildcard");
		assertThat(property.isCollectionLike(), is(true));

		TypeInformation<?> component = property.getComponentType();
		assertThat(component.isCollectionLike(), is(true));
		assertThat(component.getComponentType().getType(), is(typeCompatibleWith(String.class)));
	}

	@Test
	public void resolvesTypeParametersCorrectly() {

		TypeInformation<ConcreteType> information = ClassTypeInformation.from(ConcreteType.class);
		TypeInformation<?> superTypeInformation = information.getSuperTypeInformation(GenericType.class);

		List<TypeInformation<?>> parameters = superTypeInformation.getTypeArguments();
		assertThat(parameters, hasSize(2));
		assertThat(parameters.get(0).getType(), is((Object) String.class));
		assertThat(parameters.get(1).getType(), is((Object) Object.class));
	}

	@Test
	public void resolvesNestedInheritedTypeParameters() {

		TypeInformation<SecondExtension> information = ClassTypeInformation.from(SecondExtension.class);
		TypeInformation<?> superTypeInformation = information.getSuperTypeInformation(Base.class);

		List<TypeInformation<?>> parameters = superTypeInformation.getTypeArguments();
		assertThat(parameters, hasSize(1));
		assertThat(parameters.get(0).getType(), is((Object) String.class));
	}

	@Test
	public void discoveresMethodParameterTypesCorrectly() throws Exception {

		TypeInformation<SecondExtension> information = ClassTypeInformation.from(SecondExtension.class);
		Method method = SecondExtension.class.getMethod("foo", Base.class);
		List<TypeInformation<?>> informations = information.getParameterTypes(method);
		TypeInformation<?> returnTypeInformation = information.getReturnType(method);

		assertThat(informations, hasSize(1));
		assertThat(informations.get(0).getType(), is((Object) Base.class));
		assertThat(informations.get(0), is((Object) returnTypeInformation));
	}

	@Test
	public void discoversImplementationBindingCorrectlyForString() throws Exception {

		TypeInformation<TypedClient> information = from(TypedClient.class);
		Method method = TypedClient.class.getMethod("stringMethod", GenericInterface.class);

		TypeInformation<?> parameterType = information.getParameterTypes(method).get(0);

		TypeInformation<StringImplementation> stringInfo = from(StringImplementation.class);
		assertThat(parameterType.isAssignableFrom(stringInfo), is(true));
		assertThat(stringInfo.getSuperTypeInformation(GenericInterface.class), is((Object) parameterType));
		assertThat(parameterType.isAssignableFrom(from(LongImplementation.class)), is(false));
		assertThat(parameterType.isAssignableFrom(from(StringImplementation.class).getSuperTypeInformation(
				GenericInterface.class)), is(true));
	}

	@Test
	public void discoversImplementationBindingCorrectlyForLong() throws Exception {

		TypeInformation<TypedClient> information = from(TypedClient.class);
		Method method = TypedClient.class.getMethod("longMethod", GenericInterface.class);

		TypeInformation<?> parameterType = information.getParameterTypes(method).get(0);

		assertThat(parameterType.isAssignableFrom(from(StringImplementation.class)), is(false));
		assertThat(parameterType.isAssignableFrom(from(LongImplementation.class)), is(true));
		assertThat(parameterType.isAssignableFrom(from(StringImplementation.class).getSuperTypeInformation(
				GenericInterface.class)), is(false));
	}

	@Test
	public void discoversImplementationBindingCorrectlyForNumber() throws Exception {

		TypeInformation<TypedClient> information = from(TypedClient.class);
		Method method = TypedClient.class.getMethod("boundToNumberMethod", GenericInterface.class);

		TypeInformation<?> parameterType = information.getParameterTypes(method).get(0);

		assertThat(parameterType.isAssignableFrom(from(StringImplementation.class)), is(false));
		assertThat(parameterType.isAssignableFrom(from(LongImplementation.class)), is(true));
		assertThat(parameterType.isAssignableFrom(from(StringImplementation.class).getSuperTypeInformation(
				GenericInterface.class)), is(false));
	}

	@Test
	public void returnsComponentTypeForMultiDimensionalArrayCorrectly() {

		TypeInformation<?> information = from(String[][].class);
		assertThat(information.getType(), is((Object) String[][].class));
		assertThat(information.getComponentType().getType(), is((Object) String[].class));
		assertThat(information.getActualType().getActualType().getType(), is((Object) String.class));
	}

	@Test
	public void discoversAssignableGenericType() {

		TypeInformation<ConcreteWrapper> information = ClassTypeInformation.from(ConcreteWrapper.class);
		TypeInformation<?> wrappedType = information.getProperty("wrapped");

		assertThat(wrappedType.isAssignableFrom(ClassTypeInformation.from(ConcreteType.class)), is(true));
	}

	@Test
	public void discoveresAssignableWildcardedGenericType() {

		TypeInformation<AnotherConcreteWrapper> information = ClassTypeInformation.from(AnotherConcreteWrapper.class);
		TypeInformation<?> wrappedType = information.getProperty("wrapped");

		assertThat(wrappedType.isAssignableFrom(ClassTypeInformation.from(AnotherConcreteType.class)), is(true));
	}

	@Test
	public void discoversRawParameters() throws Exception {

		Method method = Sample.class.getMethod("setFoo", Collection.class);
		TypeInformation<?> parameterType = from(Sample.class).getParameterTypes(method).get(0);
		assertThat(parameterType.getComponentType(), is(nullValue()));

		method = Sample.class.getMethod("setBar", Collection.class);
		parameterType = from(Sample.class).getParameterTypes(method).get(0);
		assertThat(parameterType.getComponentType(), is(notNullValue()));

		method = Sample.class.getMethod("setFooBar", StringCollection.class);
		parameterType = from(Sample.class).getParameterTypes(method).get(0);
		assertThat(parameterType.getComponentType(), is(notNullValue()));
	}

	@Test
	public void returnsMapValueTypeForActualTypeIfTypeIsMap() {
		assertActualType(Locale.class, from(CustomMap.class));
	}

	@Test
	public void returnsCollctionElementTypeForActualTypeIfTypeIsCollection() {
		assertActualType(String.class, from(StringCollection.class));
	}

	@Test
	public void returnsItselfAsActualTypeForCustomGenericType() {
		assertActualType(ParameterizedType.class, from(ParameterizedType.class));
	}

	private void assertActualType(Class<?> type, TypeInformation<?> source) {

		TypeInformation<?> actualType = source.getActualType();
		assertThat(actualType, is(notNullValue()));
		assertEquals(actualType.getType(), type);
	}

	static class StringMapContainer extends MapContainer<String> {

	}

	static class MapContainer<T> {
		Map<String, T> genericMap;
		Map<String, Calendar> map;
	}

	static class StringCollectionContainer extends CollectionContainer<String> {

	}

	@SuppressWarnings("rawtypes")
	static class CollectionContainer<T> {

		T[] array;
		Collection<T>[] foo;
		Set<String> set;
		Set rawSet;
	}

	static class GenericTypeWithBound<T extends Person> {

		T person;
	}

	static class AnotherGenericType<T extends Person, S extends GenericTypeWithBound<T>> {
		S nested;
	}

	static class SpecialGenericTypeWithBound extends GenericTypeWithBound<SpecialPerson> {

	}

	static abstract class SpecialPerson extends Person {
		protected SpecialPerson(Integer ssn, String firstName, String lastName) {
			super(ssn, firstName, lastName);
		}
	}

	static class GenericType<T, S> {

		Long index;
		T content;
	}

	static class ConcreteType extends GenericType<String, Object> {

	}

	static class AnotherConcreteType extends GenericType<Long, Object> {

	}

	static class GenericWrapper<S> {

		GenericType<S, Object> wrapped;
		GenericType<? extends S, Object> wildcardWrapped;
	}

	static class ConcreteWrapper extends GenericWrapper<String> {

	}

	static class AnotherConcreteWrapper extends GenericWrapper<Long> {

	}

	static class PropertyGetter {
		private String _name;

		public byte[] getName() {
			return _name.getBytes();
		}
	}

	static class ClassWithWildCardBound {
		List<? extends String> wildcard;
		List<? extends Collection<? extends String>> complexWildcard;
	}

	static class Base<T> {

	}

	static class FirstExtension<T> extends Base<String> {

		public Base<GenericWrapper<T>> foo(Base<GenericWrapper<T>> param) {
			return null;
		}
	}

	static class SecondExtension extends FirstExtension<Long> {

	}

	interface GenericInterface<T> {

	}

	interface TypedClient {

		void stringMethod(GenericInterface<String> param);

		void longMethod(GenericInterface<Long> param);

		void boundToNumberMethod(GenericInterface<? extends Number> param);
	}

	interface CustomMap extends Map<String, Locale> {

	}

	interface StringCollection extends Collection<String> {

	}

	interface Sample {

		@SuppressWarnings("rawtypes")
		void setFoo(Collection collection);

		void setBar(Collection<String> collection);

		void setFooBar(StringCollection collection);
	}

	class StringImplementation implements GenericInterface<String> {

	}

	class LongImplementation implements GenericInterface<Long> {

	}

	static interface ThreeGenericTypes<S, T, U> {

	}

	static interface ParameterizedType extends ThreeGenericTypes<String, Integer, Long> {

	}
}
