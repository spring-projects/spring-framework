/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.*;
import static org.springframework.core.CollectionFactory.*;

/**
 * Unit tests for {@link CollectionFactory}.
 *
 * @author Oliver Gierke
 * @author Sam Brannen
 * @since 4.1.4
 */
public class CollectionFactoryTests {

	/**
	 * The test demonstrates that the generics-based API for
	 * {@link CollectionFactory#createApproximateCollection(Object, int)}
	 * is not type-safe.
	 * <p>Specifically, the parameterized type {@code E} is not bound to
	 * the type of elements contained in the {@code collection} argument
	 * passed to {@code createApproximateCollection()}. Thus casting the
	 * value returned by {@link EnumSet#copyOf(EnumSet)} to
	 * {@code (Collection<E>)} cannot guarantee that the returned collection
	 * actually contains elements of type {@code E}.
	 */
	@Test
	public void createApproximateCollectionIsNotTypeSafeForEnumSet() {
		Collection<Integer> ints = createApproximateCollection(EnumSet.of(Color.BLUE), 3);

		// Use a try-catch block to ensure that the exception is thrown as a result of the
		// next line and not as a result of the previous line.
		try {
			// Note that ints is of type Collection<Integer>, but the collection returned
			// by createApproximateCollection() is of type Collection<Color>. Thus, 42
			// cannot be cast to a Color.
			ints.add(42);
			fail("Should have thrown a ClassCastException");
		}
		catch (ClassCastException e) {
			/* expected */
		}
	}

	@Test
	public void createCollectionIsNotTypeSafeForEnumSet() {
		Collection<Integer> ints = createCollection(EnumSet.class, Color.class, 3);

		// Use a try-catch block to ensure that the exception is thrown as a result of the
		// next line and not as a result of the previous line.
		try {
			// Note that ints is of type Collection<Integer>, but the collection returned
			// by createCollection() is of type Collection<Color>. Thus, 42 cannot be cast
			// to a Color.
			ints.add(42);
			fail("Should have thrown a ClassCastException");
		}
		catch (ClassCastException e) {
			/* expected */
		}
	}

	/**
	 * The test demonstrates that the generics-based API for
	 * {@link CollectionFactory#createApproximateMap(Object, int)}
	 * is not type-safe.
	 * <p>The reasoning is similar that described in
	 * {@link #createApproximateCollectionIsNotTypeSafe()}.
	 */
	@Test
	public void createApproximateMapIsNotTypeSafeForEnumMap() {
		EnumMap<Color, Integer> enumMap = new EnumMap<>(Color.class);
		enumMap.put(Color.RED, 1);
		enumMap.put(Color.BLUE, 2);
		Map<String, Integer> map = createApproximateMap(enumMap, 3);

		// Use a try-catch block to ensure that the exception is thrown as a result of the
		// next line and not as a result of the previous line.
		try {
			// Note that the 'map' key must be of type String, but the keys in the map
			// returned by createApproximateMap() are of type Color. Thus "foo" cannot be
			// cast to a Color.
			map.put("foo", 1);
			fail("Should have thrown a ClassCastException");
		}
		catch (ClassCastException e) {
			/* expected */
		}
	}

	@Test
	public void createMapIsNotTypeSafeForEnumMap() {
		Map<String, Integer> map = createMap(EnumMap.class, Color.class, 3);

		// Use a try-catch block to ensure that the exception is thrown as a result of the
		// next line and not as a result of the previous line.
		try {
			// Note that the 'map' key must be of type String, but the keys in the map
			// returned by createMap() are of type Color. Thus "foo" cannot be cast to a
			// Color.
			map.put("foo", 1);
			fail("Should have thrown a ClassCastException");
		}
		catch (ClassCastException e) {
			/* expected */
		}
	}

	@Test
	public void createMapIsNotTypeSafeForLinkedMultiValueMap() {
		Map<String, Integer> map = createMap(MultiValueMap.class, null, 3);

		// Use a try-catch block to ensure that the exception is thrown as a result of the
		// next line and not as a result of the previous line.
		try {
			// Note: 'map' values must be of type Integer, but the values in the map
			// returned by createMap() are of type java.util.List. Thus 1 cannot be
			// cast to a List.
			map.put("foo", 1);
			fail("Should have thrown a ClassCastException");
		}
		catch (ClassCastException e) {
			/* expected */
		}
	}

	@Test
	public void createApproximateCollectionFromEmptyHashSet() {
		Collection<String> set = createApproximateCollection(new HashSet<String>(), 2);
		assertThat(set, is(empty()));
	}

	@Test
	public void createApproximateCollectionFromNonEmptyHashSet() {
		HashSet<String> hashSet = new HashSet<>();
		hashSet.add("foo");
		Collection<String> set = createApproximateCollection(hashSet, 2);
		assertThat(set, is(empty()));
	}

	@Test
	public void createApproximateCollectionFromEmptyEnumSet() {
		Collection<Color> colors = createApproximateCollection(EnumSet.noneOf(Color.class), 2);
		assertThat(colors, is(empty()));
	}

	@Test
	public void createApproximateCollectionFromNonEmptyEnumSet() {
		Collection<Color> colors = createApproximateCollection(EnumSet.of(Color.BLUE), 2);
		assertThat(colors, is(empty()));
	}

	@Test
	public void createApproximateMapFromEmptyHashMap() {
		Map<String, String> map = createApproximateMap(new HashMap<String, String>(), 2);
		assertThat(map.size(), is(0));
	}

	@Test
	public void createApproximateMapFromNonEmptyHashMap() {
		Map<String, String> hashMap = new HashMap<>();
		hashMap.put("foo", "bar");
		Map<String, String> map = createApproximateMap(hashMap, 2);
		assertThat(map.size(), is(0));
	}

	@Test
	public void createApproximateMapFromEmptyEnumMap() {
		Map<Color, String> colors = createApproximateMap(new EnumMap<Color, String>(Color.class), 2);
		assertThat(colors.size(), is(0));
	}

	@Test
	public void createApproximateMapFromNonEmptyEnumMap() {
		EnumMap<Color, String> enumMap = new EnumMap<>(Color.class);
		enumMap.put(Color.BLUE, "blue");
		Map<Color, String> colors = createApproximateMap(enumMap, 2);
		assertThat(colors.size(), is(0));
	}

	@Test
	public void createsCollectionsCorrectly() {
		// interfaces
		assertThat(createCollection(List.class, 0), is(instanceOf(ArrayList.class)));
		assertThat(createCollection(Set.class, 0), is(instanceOf(LinkedHashSet.class)));
		assertThat(createCollection(Collection.class, 0), is(instanceOf(LinkedHashSet.class)));
		assertThat(createCollection(SortedSet.class, 0), is(instanceOf(TreeSet.class)));
		assertThat(createCollection(NavigableSet.class, 0), is(instanceOf(TreeSet.class)));

		assertThat(createCollection(List.class, String.class, 0), is(instanceOf(ArrayList.class)));
		assertThat(createCollection(Set.class, String.class, 0), is(instanceOf(LinkedHashSet.class)));
		assertThat(createCollection(Collection.class, String.class, 0), is(instanceOf(LinkedHashSet.class)));
		assertThat(createCollection(SortedSet.class, String.class, 0), is(instanceOf(TreeSet.class)));
		assertThat(createCollection(NavigableSet.class, String.class, 0), is(instanceOf(TreeSet.class)));

		// concrete types
		assertThat(createCollection(HashSet.class, 0), is(instanceOf(HashSet.class)));
		assertThat(createCollection(HashSet.class, String.class, 0), is(instanceOf(HashSet.class)));
	}

	@Test
	public void createsEnumSet() {
		assertThat(createCollection(EnumSet.class, Color.class, 0), is(instanceOf(EnumSet.class)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidElementTypeForEnumSet() {
		createCollection(EnumSet.class, Object.class, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullElementTypeForEnumSet() {
		createCollection(EnumSet.class, null, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullCollectionType() {
		createCollection(null, Object.class, 0);
	}

	@Test
	public void createsMapsCorrectly() {
		// interfaces
		assertThat(createMap(Map.class, 0), is(instanceOf(LinkedHashMap.class)));
		assertThat(createMap(SortedMap.class, 0), is(instanceOf(TreeMap.class)));
		assertThat(createMap(NavigableMap.class, 0), is(instanceOf(TreeMap.class)));
		assertThat(createMap(MultiValueMap.class, 0), is(instanceOf(LinkedMultiValueMap.class)));

		assertThat(createMap(Map.class, String.class, 0), is(instanceOf(LinkedHashMap.class)));
		assertThat(createMap(SortedMap.class, String.class, 0), is(instanceOf(TreeMap.class)));
		assertThat(createMap(NavigableMap.class, String.class, 0), is(instanceOf(TreeMap.class)));
		assertThat(createMap(MultiValueMap.class, String.class, 0), is(instanceOf(LinkedMultiValueMap.class)));

		// concrete types
		assertThat(createMap(HashMap.class, 0), is(instanceOf(HashMap.class)));

		assertThat(createMap(HashMap.class, String.class, 0), is(instanceOf(HashMap.class)));
	}

	@Test
	public void createsEnumMap() {
		assertThat(createMap(EnumMap.class, Color.class, 0), is(instanceOf(EnumMap.class)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidKeyTypeForEnumMap() {
		createMap(EnumMap.class, Object.class, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullKeyTypeForEnumMap() {
		createMap(EnumMap.class, null, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullMapType() {
		createMap(null, Object.class, 0);
	}


	static enum Color {
		RED, BLUE;
	}
}
