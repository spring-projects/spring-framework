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

package org.springframework.beans;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.core.io.UrlResource;
import org.springframework.tests.sample.beans.GenericBean;
import org.springframework.tests.sample.beans.GenericIntegerBean;
import org.springframework.tests.sample.beans.GenericSetOfIntegerBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 18.01.2006
 */
public class BeanWrapperGenericsTests {

	@Test
	public void testGenericSet() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		Set<String> input = new HashSet<>();
		input.add("4");
		input.add("5");
		bw.setPropertyValue("integerSet", input);
		assertTrue(gb.getIntegerSet().contains(new Integer(4)));
		assertTrue(gb.getIntegerSet().contains(new Integer(5)));
	}

	@Test
	public void testGenericLowerBoundedSet() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.registerCustomEditor(Number.class, new CustomNumberEditor(Integer.class, true));
		Set<String> input = new HashSet<>();
		input.add("4");
		input.add("5");
		bw.setPropertyValue("numberSet", input);
		assertTrue(gb.getNumberSet().contains(new Integer(4)));
		assertTrue(gb.getNumberSet().contains(new Integer(5)));
	}

	@Test
	public void testGenericSetWithConversionFailure() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		Set<TestBean> input = new HashSet<>();
		input.add(new TestBean());
		try {
			bw.setPropertyValue("integerSet", input);
			fail("Should have thrown TypeMismatchException");
		}
		catch (TypeMismatchException ex) {
			assertTrue(ex.getMessage().indexOf("java.lang.Integer") != -1);
		}
	}

	@Test
	public void testGenericList() throws MalformedURLException {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		List<String> input = new ArrayList<>();
		input.add("http://localhost:8080");
		input.add("http://localhost:9090");
		bw.setPropertyValue("resourceList", input);
		assertEquals(new UrlResource("http://localhost:8080"), gb.getResourceList().get(0));
		assertEquals(new UrlResource("http://localhost:9090"), gb.getResourceList().get(1));
	}

	@Test
	public void testGenericListElement() throws MalformedURLException {
		GenericBean<?> gb = new GenericBean<>();
		gb.setResourceList(new ArrayList<>());
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("resourceList[0]", "http://localhost:8080");
		assertEquals(new UrlResource("http://localhost:8080"), gb.getResourceList().get(0));
	}

	@Test
	public void testGenericMap() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		Map<String, String> input = new HashMap<>();
		input.put("4", "5");
		input.put("6", "7");
		bw.setPropertyValue("shortMap", input);
		assertEquals(new Integer(5), gb.getShortMap().get(new Short("4")));
		assertEquals(new Integer(7), gb.getShortMap().get(new Short("6")));
	}

	@Test
	public void testGenericMapElement() {
		GenericBean<?> gb = new GenericBean<>();
		gb.setShortMap(new HashMap<>());
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("shortMap[4]", "5");
		assertEquals(new Integer(5), bw.getPropertyValue("shortMap[4]"));
		assertEquals(new Integer(5), gb.getShortMap().get(new Short("4")));
	}

	@Test
	public void testGenericMapWithKeyType() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		Map<String, String> input = new HashMap<>();
		input.put("4", "5");
		input.put("6", "7");
		bw.setPropertyValue("longMap", input);
		assertEquals("5", gb.getLongMap().get(new Long("4")));
		assertEquals("7", gb.getLongMap().get(new Long("6")));
	}

	@Test
	public void testGenericMapElementWithKeyType() {
		GenericBean<?> gb = new GenericBean<>();
		gb.setLongMap(new HashMap<Long, Integer>());
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("longMap[4]", "5");
		assertEquals("5", gb.getLongMap().get(new Long("4")));
		assertEquals("5", bw.getPropertyValue("longMap[4]"));
	}

	@Test
	public void testGenericMapWithCollectionValue() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.registerCustomEditor(Number.class, new CustomNumberEditor(Integer.class, false));
		Map<String, Collection> input = new HashMap<>();
		HashSet<Integer> value1 = new HashSet<>();
		value1.add(new Integer(1));
		input.put("1", value1);
		ArrayList<Boolean> value2 = new ArrayList<>();
		value2.add(Boolean.TRUE);
		input.put("2", value2);
		bw.setPropertyValue("collectionMap", input);
		assertTrue(gb.getCollectionMap().get(new Integer(1)) instanceof HashSet);
		assertTrue(gb.getCollectionMap().get(new Integer(2)) instanceof ArrayList);
	}

	@Test
	public void testGenericMapElementWithCollectionValue() {
		GenericBean<?> gb = new GenericBean<>();
		gb.setCollectionMap(new HashMap<>());
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.registerCustomEditor(Number.class, new CustomNumberEditor(Integer.class, false));
		HashSet<Integer> value1 = new HashSet<>();
		value1.add(new Integer(1));
		bw.setPropertyValue("collectionMap[1]", value1);
		assertTrue(gb.getCollectionMap().get(new Integer(1)) instanceof HashSet);
	}

	@Test
	public void testGenericMapFromProperties() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		Properties input = new Properties();
		input.setProperty("4", "5");
		input.setProperty("6", "7");
		bw.setPropertyValue("shortMap", input);
		assertEquals(new Integer(5), gb.getShortMap().get(new Short("4")));
		assertEquals(new Integer(7), gb.getShortMap().get(new Short("6")));
	}

	@Test
	public void testGenericListOfLists() throws MalformedURLException {
		GenericBean<String> gb = new GenericBean<>();
		List<List<Integer>> list = new LinkedList<>();
		list.add(new LinkedList<>());
		gb.setListOfLists(list);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("listOfLists[0][0]", new Integer(5));
		assertEquals(new Integer(5), bw.getPropertyValue("listOfLists[0][0]"));
		assertEquals(new Integer(5), gb.getListOfLists().get(0).get(0));
	}

	@Test
	public void testGenericListOfListsWithElementConversion() throws MalformedURLException {
		GenericBean<String> gb = new GenericBean<>();
		List<List<Integer>> list = new LinkedList<>();
		list.add(new LinkedList<>());
		gb.setListOfLists(list);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("listOfLists[0][0]", "5");
		assertEquals(new Integer(5), bw.getPropertyValue("listOfLists[0][0]"));
		assertEquals(new Integer(5), gb.getListOfLists().get(0).get(0));
	}

	@Test
	public void testGenericListOfArrays() throws MalformedURLException {
		GenericBean<String> gb = new GenericBean<>();
		ArrayList<String[]> list = new ArrayList<>();
		list.add(new String[] {"str1", "str2"});
		gb.setListOfArrays(list);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("listOfArrays[0][1]", "str3 ");
		assertEquals("str3 ", bw.getPropertyValue("listOfArrays[0][1]"));
		assertEquals("str3 ", gb.getListOfArrays().get(0)[1]);
	}

	@Test
	public void testGenericListOfArraysWithElementConversion() throws MalformedURLException {
		GenericBean<String> gb = new GenericBean<>();
		ArrayList<String[]> list = new ArrayList<>();
		list.add(new String[] {"str1", "str2"});
		gb.setListOfArrays(list);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.registerCustomEditor(String.class, new StringTrimmerEditor(false));
		bw.setPropertyValue("listOfArrays[0][1]", "str3 ");
		assertEquals("str3", bw.getPropertyValue("listOfArrays[0][1]"));
		assertEquals("str3", gb.getListOfArrays().get(0)[1]);
	}

	@Test
	public void testGenericListOfMaps() throws MalformedURLException {
		GenericBean<String> gb = new GenericBean<>();
		List<Map<Integer, Long>> list = new LinkedList<>();
		list.add(new HashMap<>());
		gb.setListOfMaps(list);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("listOfMaps[0][10]", new Long(5));
		assertEquals(new Long(5), bw.getPropertyValue("listOfMaps[0][10]"));
		assertEquals(new Long(5), gb.getListOfMaps().get(0).get(10));
	}

	@Test
	public void testGenericListOfMapsWithElementConversion() throws MalformedURLException {
		GenericBean<String> gb = new GenericBean<>();
		List<Map<Integer, Long>> list = new LinkedList<>();
		list.add(new HashMap<>());
		gb.setListOfMaps(list);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("listOfMaps[0][10]", "5");
		assertEquals(new Long(5), bw.getPropertyValue("listOfMaps[0][10]"));
		assertEquals(new Long(5), gb.getListOfMaps().get(0).get(10));
	}

	@Test
	public void testGenericMapOfMaps() throws MalformedURLException {
		GenericBean<String> gb = new GenericBean<>();
		Map<String, Map<Integer, Long>> map = new HashMap<>();
		map.put("mykey", new HashMap<>());
		gb.setMapOfMaps(map);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfMaps[mykey][10]", new Long(5));
		assertEquals(new Long(5), bw.getPropertyValue("mapOfMaps[mykey][10]"));
		assertEquals(new Long(5), gb.getMapOfMaps().get("mykey").get(10));
	}

	@Test
	public void testGenericMapOfMapsWithElementConversion() throws MalformedURLException {
		GenericBean<String> gb = new GenericBean<>();
		Map<String, Map<Integer, Long>> map = new HashMap<>();
		map.put("mykey", new HashMap<>());
		gb.setMapOfMaps(map);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfMaps[mykey][10]", "5");
		assertEquals(new Long(5), bw.getPropertyValue("mapOfMaps[mykey][10]"));
		assertEquals(new Long(5), gb.getMapOfMaps().get("mykey").get(10));
	}

	@Test
	public void testGenericMapOfLists() throws MalformedURLException {
		GenericBean<String> gb = new GenericBean<>();
		Map<Integer, List<Integer>> map = new HashMap<>();
		map.put(new Integer(1), new LinkedList<>());
		gb.setMapOfLists(map);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfLists[1][0]", new Integer(5));
		assertEquals(new Integer(5), bw.getPropertyValue("mapOfLists[1][0]"));
		assertEquals(new Integer(5), gb.getMapOfLists().get(new Integer(1)).get(0));
	}

	@Test
	public void testGenericMapOfListsWithElementConversion() throws MalformedURLException {
		GenericBean<String> gb = new GenericBean<>();
		Map<Integer, List<Integer>> map = new HashMap<>();
		map.put(new Integer(1), new LinkedList<>());
		gb.setMapOfLists(map);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfLists[1][0]", "5");
		assertEquals(new Integer(5), bw.getPropertyValue("mapOfLists[1][0]"));
		assertEquals(new Integer(5), gb.getMapOfLists().get(new Integer(1)).get(0));
	}

	@Test
	public void testGenericTypeNestingMapOfInteger() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("testKey", "100");

		NestedGenericCollectionBean gb = new NestedGenericCollectionBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfInteger", map);

		Object obj = gb.getMapOfInteger().get("testKey");
		assertTrue(obj instanceof Integer);
	}

	@Test
	public void testGenericTypeNestingMapOfListOfInteger() throws Exception {
		Map<String, List<String>> map = new HashMap<>();
		List<String> list = Arrays.asList(new String[] {"1", "2", "3"});
		map.put("testKey", list);

		NestedGenericCollectionBean gb = new NestedGenericCollectionBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfListOfInteger", map);

		Object obj = gb.getMapOfListOfInteger().get("testKey").get(0);
		assertTrue(obj instanceof Integer);
		assertEquals(1, ((Integer) obj).intValue());
	}

	@Test
	public void testGenericTypeNestingListOfMapOfInteger() throws Exception {
		List<Map<String, String>> list = new LinkedList<>();
		Map<String, String> map = new HashMap<>();
		map.put("testKey", "5");
		list.add(map);

		NestedGenericCollectionBean gb = new NestedGenericCollectionBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("listOfMapOfInteger", list);

		Object obj = gb.getListOfMapOfInteger().get(0).get("testKey");
		assertTrue(obj instanceof Integer);
		assertEquals(5, ((Integer) obj).intValue());
	}

	@Test
	public void testGenericTypeNestingMapOfListOfListOfInteger() throws Exception {
		Map<String, List<List<String>>> map = new HashMap<>();
		List<String> list = Arrays.asList(new String[] {"1", "2", "3"});
		map.put("testKey", Collections.singletonList(list));

		NestedGenericCollectionBean gb = new NestedGenericCollectionBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfListOfListOfInteger", map);

		Object obj = gb.getMapOfListOfListOfInteger().get("testKey").get(0).get(0);
		assertTrue(obj instanceof Integer);
		assertEquals(1, ((Integer) obj).intValue());
	}

	@Test
	public void testComplexGenericMap() {
		Map<List<String>, List<String>> inputMap = new HashMap<>();
		List<String> inputKey = new LinkedList<>();
		inputKey.add("1");
		List<String> inputValue = new LinkedList<>();
		inputValue.add("10");
		inputMap.put(inputKey, inputValue);

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("genericMap", inputMap);

		assertEquals(new Integer(1), holder.getGenericMap().keySet().iterator().next().get(0));
		assertEquals(new Long(10), holder.getGenericMap().values().iterator().next().get(0));
	}

	@Test
	public void testComplexGenericMapWithCollectionConversion() {
		Map<Set<String>, Set<String>> inputMap = new HashMap<>();
		Set<String> inputKey = new HashSet<>();
		inputKey.add("1");
		Set<String> inputValue = new HashSet<>();
		inputValue.add("10");
			inputMap.put(inputKey, inputValue);

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("genericMap", inputMap);

		assertEquals(new Integer(1), holder.getGenericMap().keySet().iterator().next().get(0));
		assertEquals(new Long(10), holder.getGenericMap().values().iterator().next().get(0));
	}

	@Test
	public void testComplexGenericIndexedMapEntry() {
		List<String> inputValue = new LinkedList<>();
		inputValue.add("10");

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("genericIndexedMap[1]", inputValue);

		assertEquals(new Integer(1), holder.getGenericIndexedMap().keySet().iterator().next());
		assertEquals(new Long(10), holder.getGenericIndexedMap().values().iterator().next().get(0));
	}

	@Test
	public void testComplexGenericIndexedMapEntryWithCollectionConversion() {
		Set<String> inputValue = new HashSet<>();
		inputValue.add("10");

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("genericIndexedMap[1]", inputValue);

		assertEquals(new Integer(1), holder.getGenericIndexedMap().keySet().iterator().next());
		assertEquals(new Long(10), holder.getGenericIndexedMap().values().iterator().next().get(0));
	}

	@Test
	public void testComplexDerivedIndexedMapEntry() {
		List<String> inputValue = new LinkedList<>();
		inputValue.add("10");

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("derivedIndexedMap[1]", inputValue);

		assertEquals(new Integer(1), holder.getDerivedIndexedMap().keySet().iterator().next());
		assertEquals(new Long(10), holder.getDerivedIndexedMap().values().iterator().next().get(0));
	}

	@Test
	public void testComplexDerivedIndexedMapEntryWithCollectionConversion() {
		Set<String> inputValue = new HashSet<>();
		inputValue.add("10");

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("derivedIndexedMap[1]", inputValue);

		assertEquals(new Integer(1), holder.getDerivedIndexedMap().keySet().iterator().next());
		assertEquals(new Long(10), holder.getDerivedIndexedMap().values().iterator().next().get(0));
	}

	@Test
	public void testGenericallyTypedIntegerBean() throws Exception {
		GenericIntegerBean gb = new GenericIntegerBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("genericProperty", "10");
		bw.setPropertyValue("genericListProperty", new String[] {"20", "30"});
		assertEquals(new Integer(10), gb.getGenericProperty());
		assertEquals(new Integer(20), gb.getGenericListProperty().get(0));
		assertEquals(new Integer(30), gb.getGenericListProperty().get(1));
	}

	@Test
	public void testGenericallyTypedSetOfIntegerBean() throws Exception {
		GenericSetOfIntegerBean gb = new GenericSetOfIntegerBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("genericProperty", "10");
		bw.setPropertyValue("genericListProperty", new String[] {"20", "30"});
		assertEquals(new Integer(10), gb.getGenericProperty().iterator().next());
		assertEquals(new Integer(20), gb.getGenericListProperty().get(0).iterator().next());
		assertEquals(new Integer(30), gb.getGenericListProperty().get(1).iterator().next());
	}

	@Test
	public void testSettingGenericPropertyWithReadOnlyInterface() {
		Bar bar = new Bar();
		BeanWrapper bw = new BeanWrapperImpl(bar);
		bw.setPropertyValue("version", "10");
		assertEquals(new Double(10.0), bar.getVersion());
	}

	@Test
	public void testSettingLongPropertyWithGenericInterface() {
		Promotion bean = new Promotion();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.setPropertyValue("id", "10");
		assertEquals(new Long(10), bean.getId());
	}

	@Test
	public void testUntypedPropertyWithMapAtRuntime() {
		class Holder<D> {
			private final D data;
			public Holder(D data) {
				this.data = data;
			}
			public D getData() {
				return this.data;
			}
		}

		Map<String, Object> data = new HashMap<>();
		data.put("x", "y");
		Holder<Map<String, Object>> context = new Holder<>(data);

		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(context);
		assertEquals("y", bw.getPropertyValue("data['x']"));

		bw.setPropertyValue("data['message']", "it works!");
		assertEquals(data.get("message"), "it works!");
	}


	private static abstract class BaseGenericCollectionBean {

		public abstract Object getMapOfInteger();

		public abstract Map<String, List<Integer>> getMapOfListOfInteger();

		public abstract void setMapOfListOfInteger(Map<String, List<Integer>> mapOfListOfInteger);
	}


	@SuppressWarnings("unused")
	private static class NestedGenericCollectionBean extends BaseGenericCollectionBean {

		private Map<String, Integer> mapOfInteger;

		private Map<String, List<Integer>> mapOfListOfInteger;

		private List<Map<String, Integer>> listOfMapOfInteger;

		private Map<String, List<List<Integer>>> mapOfListOfListOfInteger;

		@Override
		public Map<String, Integer> getMapOfInteger() {
			return mapOfInteger;
		}

		public void setMapOfInteger(Map<String, Integer> mapOfInteger) {
			this.mapOfInteger = mapOfInteger;
		}

		@Override
		public Map<String, List<Integer>> getMapOfListOfInteger() {
			return mapOfListOfInteger;
		}

		@Override
		public void setMapOfListOfInteger(Map<String, List<Integer>> mapOfListOfInteger) {
			this.mapOfListOfInteger = mapOfListOfInteger;
		}

		public List<Map<String, Integer>> getListOfMapOfInteger() {
			return listOfMapOfInteger;
		}

		public void setListOfMapOfInteger(List<Map<String, Integer>> listOfMapOfInteger) {
			this.listOfMapOfInteger = listOfMapOfInteger;
		}

		public Map<String, List<List<Integer>>> getMapOfListOfListOfInteger() {
			return mapOfListOfListOfInteger;
		}

		public void setMapOfListOfListOfInteger(Map<String, List<List<Integer>>> mapOfListOfListOfInteger) {
			this.mapOfListOfListOfInteger = mapOfListOfListOfInteger;
		}
	}


	@SuppressWarnings("unused")
	private static class ComplexMapHolder {

		private Map<List<Integer>, List<Long>> genericMap;

		private Map<Integer, List<Long>> genericIndexedMap = new HashMap<>();

		private DerivedMap derivedIndexedMap = new DerivedMap();

		public void setGenericMap(Map<List<Integer>, List<Long>> genericMap) {
			this.genericMap = genericMap;
		}

		public Map<List<Integer>, List<Long>> getGenericMap() {
			return genericMap;
		}

		public void setGenericIndexedMap(Map<Integer, List<Long>> genericIndexedMap) {
			this.genericIndexedMap = genericIndexedMap;
		}

		public Map<Integer, List<Long>> getGenericIndexedMap() {
			return genericIndexedMap;
		}

		public void setDerivedIndexedMap(DerivedMap derivedIndexedMap) {
			this.derivedIndexedMap = derivedIndexedMap;
		}

		public DerivedMap getDerivedIndexedMap() {
			return derivedIndexedMap;
		}
	}


	@SuppressWarnings("serial")
	private static class DerivedMap extends HashMap<Integer, List<Long>> {

	}


	public interface Foo {

		Number getVersion();
	}


	public class Bar implements Foo {

		private double version;

		@Override
		public Double getVersion() {
			return this.version;
		}

		public void setVersion(Double theDouble) {
			this.version = theDouble;
		}
	}


	public interface ObjectWithId<T extends Comparable<T>> {

	  T getId();

	  void setId(T aId);
	}


	public class Promotion implements ObjectWithId<Long> {

	  private Long id;

	  @Override
	  public Long getId() {
		  return id;
	  }

	  @Override
	  public void setId(Long aId) {
		  this.id = aId;
	  }
	}

}
