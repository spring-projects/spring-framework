/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.core.convert;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Andy Clement
 */
public class TypeDescriptorTests {

	public List<String> listOfString;

	public List<List<String>> listOfListOfString = new ArrayList<List<String>>();

	public List<List> listOfListOfUnknown = new ArrayList<List>();

	public int[] intArray;

	public List<String>[] arrayOfListOfString;

	public List<Integer> listField = new ArrayList<Integer>();

	public Map<String, Integer> mapField = new HashMap<String, Integer>();

	public Map<String, List<Integer>> nestedMapField = new HashMap<String, List<Integer>>();

	@Test
	public void forCollection() {
		List<String> list = new ArrayList<String>();
		list.add("1");
		TypeDescriptor desc = TypeDescriptor.forObject(list);
		assertEquals(String.class, desc.getElementType());
	}

	@Test
	public void forCollectionEmpty() {
		List<String> list = new ArrayList<String>();
		TypeDescriptor desc = TypeDescriptor.forObject(list);
		assertNull(desc.getElementType());
	}

	@Test
	public void forCollectionSuperClassCommonType() throws SecurityException, NoSuchFieldException {
		List<Number> list = new ArrayList<Number>();
		list.add(1);
		list.add(2L);
		TypeDescriptor desc = TypeDescriptor.forObject(list);
		assertEquals(Number.class, desc.getElementType());
	}

	public List<Long> longs;
	
	@Test
	public void forCollectionNoObviousCommonType() {
		List<Object> collection = new ArrayList<Object>();
		List<String> list = new ArrayList<String>();
		list.add("1");
		collection.add(list);
		Map<String, String> map = new HashMap<String, String>();
		collection.add(map);
		map.put("1", "2");
		TypeDescriptor desc = TypeDescriptor.forObject(collection);
		assertEquals(Cloneable.class, desc.getElementType());
	}

	@Test
	public void forCollectionNoCommonType() {
		List<Object> collection = new ArrayList<Object>();
		collection.add(new Object());
		collection.add("1");
		TypeDescriptor desc = TypeDescriptor.forObject(collection);		
		assertEquals(Object.class, desc.getElementType());
	}

	@Test
	public void forCollectionNested() {
		List<Object> collection = new ArrayList<Object>();
		collection.add(Arrays.asList("1", "2"));
		collection.add(Arrays.asList("3", "4"));
		TypeDescriptor desc = TypeDescriptor.forObject(collection);
		assertEquals(Arrays.asList("foo").getClass(), desc.getElementType());
		assertEquals(String.class, desc.getElementTypeDescriptor().getElementType());
	}

	@Test
	public void forMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("1", "2");
		TypeDescriptor desc = TypeDescriptor.forObject(map);
		assertEquals(String.class, desc.getMapKeyType());
		assertEquals(String.class, desc.getMapValueType());
	}

	@Test
	public void forMapEmpty() {
		Map<String, String> map = new HashMap<String, String>();
		TypeDescriptor desc = TypeDescriptor.forObject(map);
		assertNull(desc.getMapKeyType());
		assertNull(desc.getMapValueType());
	}

	@Test
	public void forMapCommonSuperClass() {
		Map<Number, Number> map = new HashMap<Number, Number>();
		map.put(1, 2);
		map.put(2L, 3L);
		TypeDescriptor desc = TypeDescriptor.forObject(map);
		assertEquals(Number.class, desc.getMapKeyType());
		assertEquals(Number.class, desc.getMapValueType());
	}

	@Test
	public void forMapNoObviousCommonType() {
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("1", "2");
		map.put(2, 2);
		TypeDescriptor desc = TypeDescriptor.forObject(map);
		assertEquals(Comparable.class, desc.getMapKeyType());
		assertEquals(Comparable.class, desc.getMapValueType());
	}

	@Test
	public void forMapNested() {
		Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();
		map.put(1, Arrays.asList("1, 2"));
		TypeDescriptor desc = TypeDescriptor.forObject(map);
		assertEquals(Integer.class, desc.getMapKeyType());
		assertEquals(String.class, desc.getMapValueTypeDescriptor().getElementType());		
	}
	
	@Test
	public void listDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfString"));
		assertFalse(typeDescriptor.isArray());
		assertEquals(List.class, typeDescriptor.getType());
		assertEquals(String.class, typeDescriptor.getElementType());
		// TODO caught shorten these names but it is OK that they are fully qualified for now
		assertEquals("java.util.List<java.lang.String>", typeDescriptor.asString());
	}

	@Test
	public void listOfListOfStringDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfListOfString"));
		assertFalse(typeDescriptor.isArray());
		assertEquals(List.class, typeDescriptor.getType());
		assertEquals(List.class, typeDescriptor.getElementType());
		assertEquals(String.class, typeDescriptor.getElementTypeDescriptor().getElementType());
		assertEquals("java.util.List<java.util.List<java.lang.String>>", typeDescriptor.asString());
	}

	@Test
	public void listOfListOfUnknownDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfListOfUnknown"));
		assertFalse(typeDescriptor.isArray());
		assertEquals(List.class, typeDescriptor.getType());
		assertEquals(List.class, typeDescriptor.getElementType());
		assertEquals(Object.class, typeDescriptor.getElementTypeDescriptor().getElementType());
		assertEquals("java.util.List<java.util.List<java.lang.Object>>", typeDescriptor.asString());
	}

	@Test
	public void arrayTypeDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("intArray"));
		assertTrue(typeDescriptor.isArray());
		assertEquals(Integer.TYPE,typeDescriptor.getElementType());
		assertEquals("int[]",typeDescriptor.asString());
	}

	@Test
	public void buildingArrayTypeDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(int[].class);
		assertTrue(typeDescriptor.isArray());
		assertEquals(Integer.TYPE ,typeDescriptor.getElementType());
	}

	@Test
	@Ignore
	public void complexTypeDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("arrayOfListOfString"));
		assertTrue(typeDescriptor.isArray());
		assertEquals(List.class,typeDescriptor.getElementType());
		assertEquals(String.class, typeDescriptor.getElementTypeDescriptor().getElementType());
		assertEquals("java.util.List[]",typeDescriptor.asString());
	}

	@Test
	public void complexTypeDescriptor2() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("nestedMapField"));
		assertTrue(typeDescriptor.isMap());
		assertEquals(String.class,typeDescriptor.getMapKeyType());
		assertEquals(List.class, typeDescriptor.getMapValueType());
		assertEquals(Integer.class, typeDescriptor.getMapValueTypeDescriptor().getElementType());
		assertEquals("java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>", typeDescriptor.asString());
	}

	@Test
	public void testEquals() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.valueOf(String.class);
		TypeDescriptor t2 = TypeDescriptor.valueOf(String.class);
		TypeDescriptor t3 = TypeDescriptor.valueOf(Date.class);
		TypeDescriptor t4 = TypeDescriptor.valueOf(Date.class);
		TypeDescriptor t5 = TypeDescriptor.valueOf(List.class);
		TypeDescriptor t6 = TypeDescriptor.valueOf(List.class);
		TypeDescriptor t7 = TypeDescriptor.valueOf(Map.class);
		TypeDescriptor t8 = TypeDescriptor.valueOf(Map.class);
		assertEquals(t1, t2);
		assertEquals(t3, t4);
		assertEquals(t5, t6);
		assertEquals(t7, t8);
		
		TypeDescriptor t9 = new TypeDescriptor(getClass().getField("listField"));
		TypeDescriptor t10 = new TypeDescriptor(getClass().getField("listField"));
		assertEquals(t9, t10);

		TypeDescriptor t11 = new TypeDescriptor(getClass().getField("mapField"));
		TypeDescriptor t12 = new TypeDescriptor(getClass().getField("mapField"));
		assertEquals(t11, t12);
	}

}
