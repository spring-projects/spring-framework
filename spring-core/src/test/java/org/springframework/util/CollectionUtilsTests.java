/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CollectionUtils}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Sam Brannen
 */
class CollectionUtilsTests {

	@Test
	void isEmpty() {
		assertThat(CollectionUtils.isEmpty((Set<Object>) null)).isTrue();
		assertThat(CollectionUtils.isEmpty((Map<String, String>) null)).isTrue();
		assertThat(CollectionUtils.isEmpty(new HashMap<>())).isTrue();
		assertThat(CollectionUtils.isEmpty(new HashSet<>())).isTrue();

		List<Object> list = new ArrayList<>();
		list.add(new Object());
		assertThat(CollectionUtils.isEmpty(list)).isFalse();

		Map<String, String> map = new HashMap<>();
		map.put("foo", "bar");
		assertThat(CollectionUtils.isEmpty(map)).isFalse();
	}

	@Test
	void mergeArrayIntoCollection() {
		Object[] arr = new Object[] {"value1", "value2"};
		List<Comparable<?>> list = new ArrayList<>();
		list.add("value3");

		CollectionUtils.mergeArrayIntoCollection(arr, list);
		assertThat(list).containsExactly("value3", "value1", "value2");
	}

	@Test
	void mergePrimitiveArrayIntoCollection() {
		int[] arr = new int[] {1, 2};
		List<Comparable<?>> list = new ArrayList<>();
		list.add(3);

		CollectionUtils.mergeArrayIntoCollection(arr, list);
		assertThat(list).containsExactly(3, 1, 2);
	}

	@Test
	void mergePropertiesIntoMap() {
		Properties defaults = new Properties();
		defaults.setProperty("prop1", "value1");
		Properties props = new Properties(defaults);
		props.setProperty("prop2", "value2");
		props.put("prop3", 3);

		Map<String, Object> map = new HashMap<>();
		map.put("prop4", "value4");

		CollectionUtils.mergePropertiesIntoMap(props, map);
		assertThat(map.get("prop1")).isEqualTo("value1");
		assertThat(map.get("prop2")).isEqualTo("value2");
		assertThat(map.get("prop3")).isEqualTo(3);
		assertThat(map.get("prop4")).isEqualTo("value4");
	}

	@Test
	void containsWithIterator() {
		assertThat(CollectionUtils.contains((Iterator<String>) null, "myElement")).isFalse();
		assertThat(CollectionUtils.contains(List.of().iterator(), "myElement")).isFalse();

		List<String> list = Arrays.asList("myElement", null);
		assertThat(CollectionUtils.contains(list.iterator(), "myElement")).isTrue();
		assertThat(CollectionUtils.contains(list.iterator(), null)).isTrue();
	}

	@Test
	void containsWithEnumeration() {
		assertThat(CollectionUtils.contains((Enumeration<String>) null, "myElement")).isFalse();
		assertThat(CollectionUtils.contains(Collections.enumeration(List.of()), "myElement")).isFalse();

		List<String> list = Arrays.asList("myElement", null);
		Enumeration<String> enumeration = Collections.enumeration(list);
		assertThat(CollectionUtils.contains(enumeration, "myElement")).isTrue();
		assertThat(CollectionUtils.contains(enumeration, null)).isTrue();
	}

	@Test
	void containsAny() {
		List<String> source = new ArrayList<>();
		source.add("abc");
		source.add("def");
		source.add("ghi");

		List<String> candidates = new ArrayList<>();
		candidates.add("xyz");
		candidates.add("def");
		candidates.add("abc");

		assertThat(CollectionUtils.containsAny(source, candidates)).isTrue();

		candidates.remove("def");
		assertThat(CollectionUtils.containsAny(source, candidates)).isTrue();

		candidates.remove("abc");
		assertThat(CollectionUtils.containsAny(source, candidates)).isFalse();

		source.add(null);
		assertThat(CollectionUtils.containsAny(source, candidates)).isFalse();

		candidates.add(null);
		assertThat(CollectionUtils.containsAny(source, candidates)).isTrue();
	}

	@Test
	void containsInstanceWithNullCollection() {
		assertThat(CollectionUtils.containsInstance(null, this)).isFalse();
	}

	@Test
	void containsInstanceWithInstancesThatAreEqualButDistinct() {
		List<Instance> list = List.of(new Instance("fiona"));
		assertThat(CollectionUtils.containsInstance(list, new Instance("fiona"))).isFalse();
	}

	@Test
	void containsInstanceWithSameInstance() {
		Instance fiona = new Instance("fiona");
		Instance apple = new Instance("apple");

		List<Instance> list = List.of(fiona, apple);
		assertThat(CollectionUtils.containsInstance(list, fiona)).isTrue();
	}

	@Test
	void containsInstanceWithNullInstance() {
		Instance fiona = new Instance("fiona");

		List<Instance> list = List.of(fiona);
		assertThat(CollectionUtils.containsInstance(list, null)).isFalse();

		list = Arrays.asList(fiona, null);
		assertThat(CollectionUtils.containsInstance(list, null)).isTrue();
	}

	@Test
	void findFirstMatch() {
		List<String> source = new ArrayList<>();
		source.add("abc");
		source.add("def");
		source.add("ghi");

		List<String> candidates = new ArrayList<>();
		candidates.add("xyz");
		candidates.add("def");
		candidates.add("abc");

		assertThat(CollectionUtils.findFirstMatch(source, candidates)).isEqualTo("def");

		source.clear();
		source.add(null);
		assertThat(CollectionUtils.findFirstMatch(source, candidates)).isNull();

		candidates.add(null);
		assertThat(CollectionUtils.findFirstMatch(source, candidates)).isNull();
	}

	@Test
	void findValueOfType() {
		assertThat(CollectionUtils.findValueOfType(List.of(1), Integer.class)).isEqualTo(1);

		assertThat(CollectionUtils.findValueOfType(Set.of(2), Integer.class)).isEqualTo(2);
	}

	@Test
	void findValueOfTypeWithNullType() {
		assertThat(CollectionUtils.findValueOfType(List.of(1), (Class<?>) null)).isEqualTo(1);
	}

	@Test
	void findValueOfTypeWithNullCollection() {
		assertThat(CollectionUtils.findValueOfType(null, Integer.class)).isNull();
	}

	@Test
	void findValueOfTypeWithEmptyCollection() {
		assertThat(CollectionUtils.findValueOfType(List.of(), Integer.class)).isNull();
	}

	@Test
	void findValueOfTypeWithMoreThanOneValue() {
		assertThat(CollectionUtils.findValueOfType(List.of(1, 2), Integer.class)).isNull();
	}

	@Test
	void hasUniqueObject() {
		List<String> list = new ArrayList<>();
		list.add("myElement");
		list.add("myOtherElement");
		assertThat(CollectionUtils.hasUniqueObject(list)).isFalse();

		list = new ArrayList<>();
		list.add("myElement");
		assertThat(CollectionUtils.hasUniqueObject(list)).isTrue();

		list = new ArrayList<>();
		list.add("myElement");
		list.add(null);
		assertThat(CollectionUtils.hasUniqueObject(list)).isFalse();

		list = new ArrayList<>();
		list.add(null);
		list.add("myElement");
		assertThat(CollectionUtils.hasUniqueObject(list)).isFalse();

		list = new ArrayList<>();
		list.add(null);
		list.add(null);
		assertThat(CollectionUtils.hasUniqueObject(list)).isTrue();

		list = new ArrayList<>();
		list.add(null);
		assertThat(CollectionUtils.hasUniqueObject(list)).isTrue();

		list = new ArrayList<>();
		assertThat(CollectionUtils.hasUniqueObject(list)).isFalse();
	}

	@Test
	void findCommonElementType() {
		List<Integer> integerList = new ArrayList<>();
		integerList.add(1);
		integerList.add(2);

		assertThat(CollectionUtils.findCommonElementType(integerList)).isEqualTo(Integer.class);
	}

	@Test
	void findCommonElementTypeWithEmptyCollection() {
		List<Integer> emptyList = new ArrayList<>();
		assertThat(CollectionUtils.findCommonElementType(emptyList)).isNull();
	}

	@Test
	void findCommonElementTypeWithDifferentElementType() {
		List<Object> list = new ArrayList<>();
		list.add(1);
		list.add("foo");
		assertThat(CollectionUtils.findCommonElementType(list)).isNull();
	}

	@Test
	void firstElementWithSet() {
		Set<Integer> set = new HashSet<>();
		set.add(17);
		set.add(3);
		set.add(2);
		set.add(1);
		assertThat(CollectionUtils.firstElement(set)).isEqualTo(17);
	}

	@Test
	void firstElementWithSortedSet() {
		SortedSet<Integer> sortedSet = new TreeSet<>();
		sortedSet.add(17);
		sortedSet.add(3);
		sortedSet.add(2);
		sortedSet.add(1);
		assertThat(CollectionUtils.firstElement(sortedSet)).isEqualTo(1);
	}

	@Test
	void firstElementWithList() {
		List<Integer> list = new ArrayList<>();
		list.add(1);
		list.add(2);
		list.add(3);
		assertThat(CollectionUtils.firstElement(list)).isEqualTo(1);
	}

	@Test
	void lastElementWithSet() {
		Set<Integer> set = new HashSet<>();
		set.add(17);
		set.add(3);
		set.add(2);
		set.add(1);
		assertThat(CollectionUtils.lastElement(set)).isEqualTo(3);
	}

	@Test
	void lastElementWithSortedSet() {
		SortedSet<Integer> sortedSet = new TreeSet<>();
		sortedSet.add(17);
		sortedSet.add(3);
		sortedSet.add(2);
		sortedSet.add(1);
		assertThat(CollectionUtils.lastElement(sortedSet)).isEqualTo(17);
	}

	@Test
	void lastElementWithList() {
		List<Integer> list = new ArrayList<>();
		list.add(1);
		list.add(2);
		list.add(3);
		assertThat(CollectionUtils.lastElement(list)).isEqualTo(3);
	}

	@Test
	void toArray() {
		Vector<String> vector = new Vector<>();
		vector.add("foo");
		vector.add("bar");
		Enumeration<String> enumeration = vector.elements();
		assertThat(CollectionUtils.toArray(enumeration, new String[]{})).containsExactly("foo", "bar");
	}

	@Test
	void conversionOfEmptyMap() {
		MultiValueMap<String, String> asMultiValueMap = CollectionUtils.toMultiValueMap(new HashMap<>());
		assertThat(asMultiValueMap).isEmpty();
		assertThat(asMultiValueMap).isEmpty();
	}

	@Test
	void conversionOfNonEmptyMap() {
		Map<String, List<String>> wrapped = new HashMap<>();
		wrapped.put("key", Arrays.asList("first", "second"));
		MultiValueMap<String, String> asMultiValueMap = CollectionUtils.toMultiValueMap(wrapped);
		assertThat(asMultiValueMap).containsAllEntriesOf(wrapped);
	}

	@Test
	void changesValueByReference() {
		Map<String, List<String>> wrapped = new HashMap<>();
		MultiValueMap<String, String> asMultiValueMap = CollectionUtils.toMultiValueMap(wrapped);
		assertThat(asMultiValueMap).doesNotContainKeys("key");
		wrapped.put("key", new ArrayList<>());
		assertThat(asMultiValueMap).containsKey("key");
	}

	@Test
	void compositeMap() {
		Map<String, String> first = new HashMap<>();
		first.put("key1", "value1");
		first.put("key2", "value2");

		Map<String, String> second = new HashMap<>();
		second.put("key3", "value3");
		second.put("key4", "value4");

		Map<String, String> compositeMap = CollectionUtils.compositeMap(first, second);

		assertThat(compositeMap).containsKeys("key1", "key2", "key3", "key4");
		assertThat(compositeMap).containsValues("value1", "value2", "value3", "value4");
	}


	private static final class Instance {

		private final String name;

		public Instance(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(@Nullable Object rhs) {
			if (this == rhs) {
				return true;
			}
			if (rhs == null || getClass() != rhs.getClass()) {
				return false;
			}
			Instance instance = (Instance) rhs;
			return this.name.equals(instance.name);
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}
	}

}
