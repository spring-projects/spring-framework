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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link TypeDiscoverer}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class TypeDiscovererUnitTests {

	@Mock
	@SuppressWarnings("rawtypes")
	Map<TypeVariable, Type> firstMap;

	@Mock
	@SuppressWarnings("rawtypes")
	Map<TypeVariable, Type> secondMap;

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullType() {
		new TypeDiscoverer<Object>(null, null);
	}

	@Test
	public void isNotEqualIfTypesDiffer() {

		TypeDiscoverer<Object> objectTypeInfo = new TypeDiscoverer<Object>(Object.class, null);
		TypeDiscoverer<String> stringTypeInfo = new TypeDiscoverer<String>(String.class, null);

		assertFalse(objectTypeInfo.equals(stringTypeInfo));
	}

	@Test
	public void isNotEqualIfTypeVariableMapsDiffer() {

		assertFalse(firstMap.equals(secondMap));

		TypeDiscoverer<Object> first = new TypeDiscoverer<Object>(Object.class, firstMap);
		TypeDiscoverer<Object> second = new TypeDiscoverer<Object>(Object.class, secondMap);

		assertFalse(first.equals(second));
	}

	@Test
	public void dealsWithTypesReferencingThemselves() {

		TypeInformation<SelfReferencing> information = new ClassTypeInformation<SelfReferencing>(SelfReferencing.class);
		TypeInformation<?> first = information.getProperty("parent").getMapValueType();
		TypeInformation<?> second = first.getProperty("map").getMapValueType();
		assertEquals(first, second);
	}

	@Test
	public void dealsWithTypesReferencingThemselvesInAMap() {

		TypeInformation<SelfReferencingMap> information = new ClassTypeInformation<SelfReferencingMap>(
				SelfReferencingMap.class);
		TypeInformation<?> mapValueType = information.getProperty("map").getMapValueType();
		assertEquals(mapValueType, information);
	}

	@Test
	public void returnsComponentAndValueTypesForMapExtensions() {
		TypeInformation<?> discoverer = new TypeDiscoverer<Object>(CustomMap.class, null);
		assertEquals(Locale.class, discoverer.getMapValueType().getType());
		assertEquals(String.class, discoverer.getComponentType().getType());
	}

	@Test
	public void returnsComponentTypeForCollectionExtension() {
		TypeDiscoverer<CustomCollection> discoverer = new TypeDiscoverer<CustomCollection>(CustomCollection.class, null);
		TypeInformation<?> componentType = discoverer.getComponentType();
		assertThat(componentType, is(notNullValue()));
		assertEquals(String.class, componentType.getType());
	}

	@Test
	public void returnsComponentTypeForArrays() {
		TypeDiscoverer<String[]> discoverer = new TypeDiscoverer<String[]>(String[].class, null);
		assertEquals(String.class, discoverer.getComponentType().getType());
	}

	/**
	 * @see DATACMNS-57
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void discoveresConstructorParameterTypesCorrectly() throws NoSuchMethodException, SecurityException {

		TypeDiscoverer<GenericConstructors> discoverer = new TypeDiscoverer<GenericConstructors>(GenericConstructors.class,
				null);
		Constructor<GenericConstructors> constructor = GenericConstructors.class.getConstructor(List.class, Locale.class);
		List<TypeInformation<?>> types = discoverer.getParameterTypes(constructor);
		assertThat(types.size(), is(2));
		assertThat(types.get(0).getType(), equalTo((Class) List.class));
		assertThat(types.get(0).getComponentType().getType(), is(equalTo((Class) String.class)));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void returnsNullForComponentAndValueTypesForRawMaps() {
		TypeDiscoverer<Map> discoverer = new TypeDiscoverer<Map>(Map.class, null);
		assertThat(discoverer.getComponentType(), is(nullValue()));
		assertThat(discoverer.getMapValueType(), is(nullValue()));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void returnsNullForComponentAndValueTypesForRawCollections() {

		TypeDiscoverer<Collection> discoverer = new TypeDiscoverer<Collection>(Collection.class, null);
		assertThat(discoverer.getComponentType(), is(nullValue()));
	}

	/**
	 * @see DATACMNS-167
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void doesNotConsiderTypeImplementingIterableACollection() {

		TypeDiscoverer<Person> discoverer = new TypeDiscoverer<Person>(Person.class, null);
		TypeInformation reference = ClassTypeInformation.from(Address.class);

		TypeInformation<?> addresses = discoverer.getProperty("addresses");
		assertThat(addresses.isCollectionLike(), is(false));
		assertThat(addresses.getComponentType(), is(reference));

		TypeInformation<?> adressIterable = discoverer.getProperty("addressIterable");
		assertThat(adressIterable.isCollectionLike(), is(true));
		assertThat(adressIterable.getComponentType(), is(reference));
	}

	class Person {

		Addresses addresses;
		Iterable<Address> addressIterable;
	}

	abstract class Addresses implements Iterable<Address> {

	}

	class Address {

	}

	class SelfReferencing {

		Map<String, SelfReferencingMap> parent;
	}

	class SelfReferencingMap {
		Map<String, SelfReferencingMap> map;
	}

	interface CustomMap extends Map<String, Locale> {

	}

	interface CustomCollection extends Collection<String> {

	}

	public static class GenericConstructors {

		public GenericConstructors(List<String> first, Locale second) {

		}
	}
}
