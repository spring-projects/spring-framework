/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public class CollectionUtilsTests {

	@Test
	public void testIsEmpty() {
		assertTrue(CollectionUtils.isEmpty((Set<?>) null));
		assertTrue(CollectionUtils.isEmpty((Map<?, ?>) null));
		assertTrue(CollectionUtils.isEmpty(new HashMap<Object, Object>()));
		assertTrue(CollectionUtils.isEmpty(new HashSet<Object>()));

		List<Object> list = new LinkedList<Object>();
		list.add(new Object());
		assertFalse(CollectionUtils.isEmpty(list));

		HashMap<Object, Object> map = new HashMap<Object, Object>();
		map.put("foo", "bar");
		assertFalse(CollectionUtils.isEmpty(map));
	}

	@Test
	public void testMergeArrayIntoCollection() {
		Object[] arr = new Object[] {"value1", "value2"};
		List<Object> list = new LinkedList<Object>();
		list.add("value3");

		CollectionUtils.mergeArrayIntoCollection(arr, list);
		assertEquals("value3", list.get(0));
		assertEquals("value1", list.get(1));
		assertEquals("value2", list.get(2));
	}

	@Test
	public void testMergePrimitiveArrayIntoCollection() {
		int[] arr = new int[] {1, 2};
		List<Object> list = new LinkedList<Object>();
		list.add(new Integer(3));

		CollectionUtils.mergeArrayIntoCollection(arr, list);
		assertEquals(new Integer(3), list.get(0));
		assertEquals(new Integer(1), list.get(1));
		assertEquals(new Integer(2), list.get(2));
	}

	@Test
	public void testMergePropertiesIntoMap() {
		Properties defaults = new Properties();
		defaults.setProperty("prop1", "value1");
		Properties props = new Properties(defaults);
		props.setProperty("prop2", "value2");
		props.put("prop3", new Integer(3));

		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("prop4", "value4");

		CollectionUtils.mergePropertiesIntoMap(props, map);
		assertEquals("value1", map.get("prop1"));
		assertEquals("value2", map.get("prop2"));
		assertEquals(new Integer(3), map.get("prop3"));
		assertEquals("value4", map.get("prop4"));
	}

	@Test
	public void testContains() {
		assertFalse(CollectionUtils.contains((Iterator<?>) null, "myElement"));
		assertFalse(CollectionUtils.contains((Enumeration<?>) null, "myElement"));
		assertFalse(CollectionUtils.contains(new LinkedList<Object>().iterator(), "myElement"));
		assertFalse(CollectionUtils.contains(new Hashtable<Object, Object>().keys(), "myElement"));

		List<Object> list = new LinkedList<Object>();
		list.add("myElement");
		assertTrue(CollectionUtils.contains(list.iterator(), "myElement"));

		Hashtable<Object, Object> ht = new Hashtable<Object, Object>();
		ht.put("myElement", "myValue");
		assertTrue(CollectionUtils.contains(ht.keys(), "myElement"));
	}

	@Test
	public void testContainsAny() throws Exception {
		ArrayList<Object> source = new ArrayList<Object>();
		source.add("abc");
		source.add("def");
		source.add("ghi");

		ArrayList<Object> candidates = new ArrayList<Object>();
		candidates.add("xyz");
		candidates.add("def");
		candidates.add("abc");

		assertTrue(CollectionUtils.containsAny(source, candidates));
		candidates.remove("def");
		assertTrue(CollectionUtils.containsAny(source, candidates));
		candidates.remove("abc");
		assertFalse(CollectionUtils.containsAny(source, candidates));
	}

	@Test
	public void testContainsInstanceWithNullCollection() throws Exception {
		assertFalse("Must return false if supplied Collection argument is null",
				CollectionUtils.containsInstance(null, this));
	}

	@Test
	public void testContainsInstanceWithInstancesThatAreEqualButDistinct() throws Exception {
		List<Object> list = new ArrayList<Object>();
		list.add(new Instance("fiona"));
		assertFalse("Must return false if instance is not in the supplied Collection argument",
				CollectionUtils.containsInstance(list, new Instance("fiona")));
	}

	@Test
	public void testContainsInstanceWithSameInstance() throws Exception {
		List<Object> list = new ArrayList<Object>();
		list.add(new Instance("apple"));
		Instance instance = new Instance("fiona");
		list.add(instance);
		assertTrue("Must return true if instance is in the supplied Collection argument",
				CollectionUtils.containsInstance(list, instance));
	}

	@Test
	public void testContainsInstanceWithNullInstance() throws Exception {
		List<Object> list = new ArrayList<Object>();
		list.add(new Instance("apple"));
		list.add(new Instance("fiona"));
		assertFalse("Must return false if null instance is supplied",
				CollectionUtils.containsInstance(list, null));
	}

	@Test
	public void testFindFirstMatch() throws Exception {
		List<Object> source = new ArrayList<Object>();
		source.add("abc");
		source.add("def");
		source.add("ghi");

		List<Object> candidates = new ArrayList<Object>();
		candidates.add("xyz");
		candidates.add("def");
		candidates.add("abc");

		assertEquals("def", CollectionUtils.findFirstMatch(source, candidates));
	}

	@Test
	public void testHasUniqueObject() {
		List<Object> list = new LinkedList<Object>();
		list.add("myElement");
		list.add("myOtherElement");
		assertFalse(CollectionUtils.hasUniqueObject(list));

		list = new LinkedList<Object>();
		list.add("myElement");
		assertTrue(CollectionUtils.hasUniqueObject(list));

		list = new LinkedList<Object>();
		list.add("myElement");
		list.add(null);
		assertFalse(CollectionUtils.hasUniqueObject(list));

		list = new LinkedList<Object>();
		list.add(null);
		list.add("myElement");
		assertFalse(CollectionUtils.hasUniqueObject(list));

		list = new LinkedList<Object>();
		list.add(null);
		list.add(null);
		assertTrue(CollectionUtils.hasUniqueObject(list));

		list = new LinkedList<Object>();
		list.add(null);
		assertTrue(CollectionUtils.hasUniqueObject(list));

		list = new LinkedList<Object>();
		assertFalse(CollectionUtils.hasUniqueObject(list));
	}


	private static final class Instance {

		private final String name;

		public Instance(String name) {
			this.name = name;
		}

		public boolean equals(Object rhs) {
			if (this == rhs) {
				return true;
			}
			if (rhs == null || this.getClass() != rhs.getClass()) {
				return false;
			}
			Instance instance = (Instance) rhs;
			return this.name.equals(instance.name);
		}

		public int hashCode() {
			return this.name.hashCode();
		}
	}

}
