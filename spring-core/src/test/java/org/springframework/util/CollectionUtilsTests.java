/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 */
class CollectionUtilsTests {

	@Test
	void isEmpty() {
		assertThat(CollectionUtils.isEmpty((Set<Object>) null)).isTrue();
		assertThat(CollectionUtils.isEmpty((Map<String, String>) null)).isTrue();
		assertThat(CollectionUtils.isEmpty(new HashMap<String, String>())).isTrue();
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
		assertThat(list.get(0)).isEqualTo("value3");
		assertThat(list.get(1)).isEqualTo("value1");
		assertThat(list.get(2)).isEqualTo("value2");
	}

	@Test
	void mergePrimitiveArrayIntoCollection() {
		int[] arr = new int[] {1, 2};
		List<Comparable<?>> list = new ArrayList<>();
		list.add(3);

		CollectionUtils.mergeArrayIntoCollection(arr, list);
		assertThat(list.get(0)).isEqualTo(3);
		assertThat(list.get(1)).isEqualTo(1);
		assertThat(list.get(2)).isEqualTo(2);
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
	void contains() {
		assertThat(CollectionUtils.contains((Iterator<String>) null, "myElement")).isFalse();
		assertThat(CollectionUtils.contains((Enumeration<String>) null, "myElement")).isFalse();
		assertThat(CollectionUtils.contains(new ArrayList<String>().iterator(), "myElement")).isFalse();
		assertThat(CollectionUtils.contains(new Hashtable<String, Object>().keys(), "myElement")).isFalse();

		List<String> list = new ArrayList<>();
		list.add("myElement");
		assertThat(CollectionUtils.contains(list.iterator(), "myElement")).isTrue();

		Hashtable<String, String> ht = new Hashtable<>();
		ht.put("myElement", "myValue");
		assertThat(CollectionUtils.contains(ht.keys(), "myElement")).isTrue();
	}

	@Test
	void containsAny() throws Exception {
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
	}

	@Test
	void containsInstanceWithNullCollection() throws Exception {
		assertThat(CollectionUtils.containsInstance(null, this)).as("Must return false if supplied Collection argument is null").isFalse();
	}

	@Test
	void containsInstanceWithInstancesThatAreEqualButDistinct() throws Exception {
		List<Instance> list = new ArrayList<>();
		list.add(new Instance("fiona"));
		assertThat(CollectionUtils.containsInstance(list, new Instance("fiona"))).as("Must return false if instance is not in the supplied Collection argument").isFalse();
	}

	@Test
	void containsInstanceWithSameInstance() throws Exception {
		List<Instance> list = new ArrayList<>();
		list.add(new Instance("apple"));
		Instance instance = new Instance("fiona");
		list.add(instance);
		assertThat(CollectionUtils.containsInstance(list, instance)).as("Must return true if instance is in the supplied Collection argument").isTrue();
	}

	@Test
	void containsInstanceWithNullInstance() throws Exception {
		List<Instance> list = new ArrayList<>();
		list.add(new Instance("apple"));
		list.add(new Instance("fiona"));
		assertThat(CollectionUtils.containsInstance(list, null)).as("Must return false if null instance is supplied").isFalse();
	}

	@Test
	void findFirstMatch() throws Exception {
		List<String> source = new ArrayList<>();
		source.add("abc");
		source.add("def");
		source.add("ghi");

		List<String> candidates = new ArrayList<>();
		candidates.add("xyz");
		candidates.add("def");
		candidates.add("abc");

		assertThat(CollectionUtils.findFirstMatch(source, candidates)).isEqualTo("def");
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
			if (rhs == null || this.getClass() != rhs.getClass()) {
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
