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

package org.springframework.beans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.beans.testfixture.beans.GenericBean;
import org.springframework.beans.testfixture.beans.GenericIntegerBean;
import org.springframework.beans.testfixture.beans.GenericSetOfIntegerBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.UrlResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 18.01.2006
 */
class BeanWrapperGenericsTests {

	@Test
	void testGenericSet() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		Set<String> input = new HashSet<>();
		input.add("4");
		input.add("5");
		bw.setPropertyValue("integerSet", input);
		assertThat(gb.getIntegerSet().contains(4)).isTrue();
		assertThat(gb.getIntegerSet().contains(5)).isTrue();
	}

	@Test
	void testGenericLowerBoundedSet() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.registerCustomEditor(Number.class, new CustomNumberEditor(Integer.class, true));
		Set<String> input = new HashSet<>();
		input.add("4");
		input.add("5");
		bw.setPropertyValue("numberSet", input);
		assertThat(gb.getNumberSet().contains(4)).isTrue();
		assertThat(gb.getNumberSet().contains(5)).isTrue();
	}

	@Test
	void testGenericSetWithConversionFailure() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		Set<TestBean> input = new HashSet<>();
		input.add(new TestBean());
		assertThatExceptionOfType(TypeMismatchException.class).isThrownBy(() ->
				bw.setPropertyValue("integerSet", input))
			.withMessageContaining("java.lang.Integer");
	}

	@Test
	void testGenericList() throws Exception {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		List<String> input = new ArrayList<>();
		input.add("http://localhost:8080");
		input.add("http://localhost:9090");
		bw.setPropertyValue("resourceList", input);
		assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
		assertThat(gb.getResourceList().get(1)).isEqualTo(new UrlResource("http://localhost:9090"));
	}

	@Test
	void testGenericListElement() throws Exception {
		GenericBean<?> gb = new GenericBean<>();
		gb.setResourceList(new ArrayList<>());
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("resourceList[0]", "http://localhost:8080");
		assertThat(gb.getResourceList().get(0)).isEqualTo(new UrlResource("http://localhost:8080"));
	}

	@Test
	void testGenericMap() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		Map<String, String> input = new HashMap<>();
		input.put("4", "5");
		input.put("6", "7");
		bw.setPropertyValue("shortMap", input);
		assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
		assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
	}

	@Test
	void testGenericMapElement() {
		GenericBean<?> gb = new GenericBean<>();
		gb.setShortMap(new HashMap<>());
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("shortMap[4]", "5");
		assertThat(bw.getPropertyValue("shortMap[4]")).isEqualTo(5);
		assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
	}

	@Test
	void testGenericMapWithKeyType() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		Map<String, String> input = new HashMap<>();
		input.put("4", "5");
		input.put("6", "7");
		bw.setPropertyValue("longMap", input);
		assertThat(gb.getLongMap().get(4L)).isEqualTo("5");
		assertThat(gb.getLongMap().get(6L)).isEqualTo("7");
	}

	@Test
	void testGenericMapElementWithKeyType() {
		GenericBean<?> gb = new GenericBean<>();
		gb.setLongMap(new HashMap<Long, Integer>());
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("longMap[4]", "5");
		assertThat(gb.getLongMap().get(Long.valueOf("4"))).isEqualTo("5");
		assertThat(bw.getPropertyValue("longMap[4]")).isEqualTo("5");
	}

	@Test
	void testGenericMapWithCollectionValue() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.registerCustomEditor(Number.class, new CustomNumberEditor(Integer.class, false));
		Map<String, Collection<?>> input = new HashMap<>();
		HashSet<Integer> value1 = new HashSet<>();
		value1.add(1);
		input.put("1", value1);
		ArrayList<Boolean> value2 = new ArrayList<>();
		value2.add(Boolean.TRUE);
		input.put("2", value2);
		bw.setPropertyValue("collectionMap", input);
		assertThat(gb.getCollectionMap().get(1)).isInstanceOf(HashSet.class);
		assertThat(gb.getCollectionMap().get(2)).isInstanceOf(ArrayList.class);
	}

	@Test
	void testGenericMapElementWithCollectionValue() {
		GenericBean<?> gb = new GenericBean<>();
		gb.setCollectionMap(new HashMap<>());
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.registerCustomEditor(Number.class, new CustomNumberEditor(Integer.class, false));
		HashSet<Integer> value1 = new HashSet<>();
		value1.add(1);
		bw.setPropertyValue("collectionMap[1]", value1);
		assertThat(gb.getCollectionMap().get(1)).isInstanceOf(HashSet.class);
	}

	@Test
	void testGenericMapFromProperties() {
		GenericBean<?> gb = new GenericBean<>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		Properties input = new Properties();
		input.setProperty("4", "5");
		input.setProperty("6", "7");
		bw.setPropertyValue("shortMap", input);
		assertThat(gb.getShortMap().get(Short.valueOf("4"))).isEqualTo(5);
		assertThat(gb.getShortMap().get(Short.valueOf("6"))).isEqualTo(7);
	}

	@Test
	void testGenericListOfLists() {
		GenericBean<String> gb = new GenericBean<>();
		List<List<Integer>> list = new ArrayList<>();
		list.add(new ArrayList<>());
		gb.setListOfLists(list);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("listOfLists[0][0]", 5);
		assertThat(bw.getPropertyValue("listOfLists[0][0]")).isEqualTo(5);
		assertThat(gb.getListOfLists().get(0).get(0)).isEqualTo(5);
	}

	@Test
	void testGenericListOfListsWithElementConversion() {
		GenericBean<String> gb = new GenericBean<>();
		List<List<Integer>> list = new ArrayList<>();
		list.add(new ArrayList<>());
		gb.setListOfLists(list);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("listOfLists[0][0]", "5");
		assertThat(bw.getPropertyValue("listOfLists[0][0]")).isEqualTo(5);
		assertThat(gb.getListOfLists().get(0).get(0)).isEqualTo(5);
	}

	@Test
	void testGenericListOfArrays() {
		GenericBean<String> gb = new GenericBean<>();
		ArrayList<String[]> list = new ArrayList<>();
		list.add(new String[] {"str1", "str2"});
		gb.setListOfArrays(list);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("listOfArrays[0][1]", "str3 ");
		assertThat(bw.getPropertyValue("listOfArrays[0][1]")).isEqualTo("str3 ");
		assertThat(gb.getListOfArrays().get(0)[1]).isEqualTo("str3 ");
	}

	@Test
	void testGenericListOfArraysWithElementConversion() {
		GenericBean<String> gb = new GenericBean<>();
		ArrayList<String[]> list = new ArrayList<>();
		list.add(new String[] {"str1", "str2"});
		gb.setListOfArrays(list);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.registerCustomEditor(String.class, new StringTrimmerEditor(false));
		bw.setPropertyValue("listOfArrays[0][1]", "str3 ");
		assertThat(bw.getPropertyValue("listOfArrays[0][1]")).isEqualTo("str3");
		assertThat(gb.getListOfArrays().get(0)[1]).isEqualTo("str3");
	}

	@Test
	void testGenericListOfMaps() {
		GenericBean<String> gb = new GenericBean<>();
		List<Map<Integer, Long>> list = new ArrayList<>();
		list.add(new HashMap<>());
		gb.setListOfMaps(list);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("listOfMaps[0][10]", 5L);
		assertThat(bw.getPropertyValue("listOfMaps[0][10]")).isEqualTo(5L);
		assertThat(gb.getListOfMaps().get(0).get(10)).isEqualTo(Long.valueOf(5));
	}

	@Test
	void testGenericListOfMapsWithElementConversion() {
		GenericBean<String> gb = new GenericBean<>();
		List<Map<Integer, Long>> list = new ArrayList<>();
		list.add(new HashMap<>());
		gb.setListOfMaps(list);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("listOfMaps[0][10]", "5");
		assertThat(bw.getPropertyValue("listOfMaps[0][10]")).isEqualTo(5L);
		assertThat(gb.getListOfMaps().get(0).get(10)).isEqualTo(Long.valueOf(5));
	}

	@Test
	void testGenericMapOfMaps() {
		GenericBean<String> gb = new GenericBean<>();
		Map<String, Map<Integer, Long>> map = new HashMap<>();
		map.put("mykey", new HashMap<>());
		gb.setMapOfMaps(map);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfMaps[mykey][10]", 5L);
		assertThat(bw.getPropertyValue("mapOfMaps[mykey][10]")).isEqualTo(5L);
		assertThat(gb.getMapOfMaps().get("mykey").get(10)).isEqualTo(Long.valueOf(5));
	}

	@Test
	void testGenericMapOfMapsWithElementConversion() {
		GenericBean<String> gb = new GenericBean<>();
		Map<String, Map<Integer, Long>> map = new HashMap<>();
		map.put("mykey", new HashMap<>());
		gb.setMapOfMaps(map);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfMaps[mykey][10]", "5");
		assertThat(bw.getPropertyValue("mapOfMaps[mykey][10]")).isEqualTo(5L);
		assertThat(gb.getMapOfMaps().get("mykey").get(10)).isEqualTo(Long.valueOf(5));
	}

	@Test
	void testGenericMapOfLists() {
		GenericBean<String> gb = new GenericBean<>();
		Map<Integer, List<Integer>> map = new HashMap<>();
		map.put(1, new ArrayList<>());
		gb.setMapOfLists(map);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfLists[1][0]", 5);
		assertThat(bw.getPropertyValue("mapOfLists[1][0]")).isEqualTo(5);
		assertThat(gb.getMapOfLists().get(1).get(0)).isEqualTo(5);
	}

	@Test
	void testGenericMapOfListsWithElementConversion() {
		GenericBean<String> gb = new GenericBean<>();
		Map<Integer, List<Integer>> map = new HashMap<>();
		map.put(1, new ArrayList<>());
		gb.setMapOfLists(map);
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfLists[1][0]", "5");
		assertThat(bw.getPropertyValue("mapOfLists[1][0]")).isEqualTo(5);
		assertThat(gb.getMapOfLists().get(1).get(0)).isEqualTo(5);
	}

	@Test
	void testGenericTypeNestingMapOfInteger() {
		Map<String, String> map = new HashMap<>();
		map.put("testKey", "100");

		NestedGenericCollectionBean gb = new NestedGenericCollectionBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfInteger", map);

		Object obj = gb.getMapOfInteger().get("testKey");
		assertThat(obj).isInstanceOf(Integer.class);
	}

	@Test
	void testGenericTypeNestingMapOfListOfInteger() {
		Map<String, List<String>> map = new HashMap<>();
		List<String> list = Arrays.asList("1", "2", "3");
		map.put("testKey", list);

		NestedGenericCollectionBean gb = new NestedGenericCollectionBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfListOfInteger", map);

		Object obj = gb.getMapOfListOfInteger().get("testKey").get(0);
		assertThat(obj).isInstanceOf(Integer.class);
		assertThat(obj).isEqualTo(1);
	}

	@Test
	void testGenericTypeNestingListOfMapOfInteger() {
		List<Map<String, String>> list = new ArrayList<>();
		Map<String, String> map = new HashMap<>();
		map.put("testKey", "5");
		list.add(map);

		NestedGenericCollectionBean gb = new NestedGenericCollectionBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("listOfMapOfInteger", list);

		Object obj = gb.getListOfMapOfInteger().get(0).get("testKey");
		assertThat(obj).isInstanceOf(Integer.class);
		assertThat(obj).isEqualTo(5);
	}

	@Test
	void testGenericTypeNestingMapOfListOfListOfInteger() {
		Map<String, List<List<String>>> map = new HashMap<>();
		List<String> list = Arrays.asList("1", "2", "3");
		map.put("testKey", Collections.singletonList(list));

		NestedGenericCollectionBean gb = new NestedGenericCollectionBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("mapOfListOfListOfInteger", map);

		Object obj = gb.getMapOfListOfListOfInteger().get("testKey").get(0).get(0);
		assertThat(obj).isInstanceOf(Integer.class);
		assertThat(obj).isEqualTo(1);
	}

	@Test
	void testComplexGenericMap() {
		Map<List<String>, List<String>> inputMap = new HashMap<>();
		List<String> inputKey = new ArrayList<>();
		inputKey.add("1");
		List<String> inputValue = new ArrayList<>();
		inputValue.add("10");
		inputMap.put(inputKey, inputValue);

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("genericMap", inputMap);

		assertThat(holder.getGenericMap().keySet().iterator().next().get(0)).isEqualTo(1);
		assertThat(holder.getGenericMap().values().iterator().next().get(0)).isEqualTo(Long.valueOf(10));
	}

	@Test
	void testComplexGenericMapWithCollectionConversion() {
		Map<Set<String>, Set<String>> inputMap = new HashMap<>();
		Set<String> inputKey = new HashSet<>();
		inputKey.add("1");
		Set<String> inputValue = new HashSet<>();
		inputValue.add("10");
			inputMap.put(inputKey, inputValue);

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("genericMap", inputMap);

		assertThat(holder.getGenericMap().keySet().iterator().next().get(0)).isEqualTo(1);
		assertThat(holder.getGenericMap().values().iterator().next().get(0)).isEqualTo(Long.valueOf(10));
	}

	@Test
	void testComplexGenericIndexedMapEntry() {
		List<String> inputValue = new ArrayList<>();
		inputValue.add("10");

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("genericIndexedMap[1]", inputValue);

		assertThat(holder.getGenericIndexedMap().keySet().iterator().next()).isEqualTo(1);
		assertThat(holder.getGenericIndexedMap().values().iterator().next().get(0)).isEqualTo(Long.valueOf(10));
	}

	@Test
	void testComplexGenericIndexedMapEntryWithCollectionConversion() {
		Set<String> inputValue = new HashSet<>();
		inputValue.add("10");

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("genericIndexedMap[1]", inputValue);

		assertThat(holder.getGenericIndexedMap().keySet().iterator().next()).isEqualTo(1);
		assertThat(holder.getGenericIndexedMap().values().iterator().next().get(0)).isEqualTo(Long.valueOf(10));
	}

	@Test
	void testComplexGenericIndexedMapEntryWithPlainValue() {
		String inputValue = "10";

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("genericIndexedMap[1]", inputValue);

		assertThat(holder.getGenericIndexedMap().keySet().iterator().next()).isEqualTo(1);
		assertThat(holder.getGenericIndexedMap().values().iterator().next().get(0)).isEqualTo(Long.valueOf(10));
	}

	@Test
	void testComplexDerivedIndexedMapEntry() {
		List<String> inputValue = new ArrayList<>();
		inputValue.add("10");

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("derivedIndexedMap[1]", inputValue);

		assertThat(holder.getDerivedIndexedMap().keySet().iterator().next()).isEqualTo(1);
		assertThat(holder.getDerivedIndexedMap().values().iterator().next().get(0)).isEqualTo(Long.valueOf(10));
	}

	@Test
	void testComplexDerivedIndexedMapEntryWithCollectionConversion() {
		Set<String> inputValue = new HashSet<>();
		inputValue.add("10");

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("derivedIndexedMap[1]", inputValue);

		assertThat(holder.getDerivedIndexedMap().keySet().iterator().next()).isEqualTo(1);
		assertThat(holder.getDerivedIndexedMap().values().iterator().next().get(0)).isEqualTo(Long.valueOf(10));
	}

	@Test
	void testComplexDerivedIndexedMapEntryWithPlainValue() {
		String inputValue = "10";

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("derivedIndexedMap[1]", inputValue);

		assertThat(holder.getDerivedIndexedMap().keySet().iterator().next()).isEqualTo(1);
		assertThat(holder.getDerivedIndexedMap().values().iterator().next().get(0)).isEqualTo(Long.valueOf(10));
	}

	@Test
	void testComplexMultiValueMapEntry() {
		List<String> inputValue = new ArrayList<>();
		inputValue.add("10");

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("multiValueMap[1]", inputValue);

		assertThat(holder.getMultiValueMap().keySet().iterator().next()).isEqualTo(1);
		assertThat(holder.getMultiValueMap().values().iterator().next().get(0)).isEqualTo(Long.valueOf(10));
	}

	@Test
	void testComplexMultiValueMapEntryWithCollectionConversion() {
		Set<String> inputValue = new HashSet<>();
		inputValue.add("10");

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("multiValueMap[1]", inputValue);

		assertThat(holder.getMultiValueMap().keySet().iterator().next()).isEqualTo(1);
		assertThat(holder.getMultiValueMap().values().iterator().next().get(0)).isEqualTo(Long.valueOf(10));
	}

	@Test
	void testComplexMultiValueMapEntryWithPlainValue() {
		String inputValue = "10";

		ComplexMapHolder holder = new ComplexMapHolder();
		BeanWrapper bw = new BeanWrapperImpl(holder);
		bw.setPropertyValue("multiValueMap[1]", inputValue);

		assertThat(holder.getMultiValueMap().keySet().iterator().next()).isEqualTo(1);
		assertThat(holder.getMultiValueMap().values().iterator().next().get(0)).isEqualTo(Long.valueOf(10));
	}

	@Test
	void testGenericallyTypedIntegerBean() {
		GenericIntegerBean gb = new GenericIntegerBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("genericProperty", "10");
		bw.setPropertyValue("genericListProperty", new String[] {"20", "30"});
		assertThat(gb.getGenericProperty()).isEqualTo(10);
		assertThat(gb.getGenericListProperty().get(0)).isEqualTo(20);
		assertThat(gb.getGenericListProperty().get(1)).isEqualTo(30);
	}

	@Test
	void testGenericallyTypedSetOfIntegerBean() {
		GenericSetOfIntegerBean gb = new GenericSetOfIntegerBean();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("genericProperty", "10");
		bw.setPropertyValue("genericListProperty", new String[] {"20", "30"});
		assertThat(gb.getGenericProperty().iterator().next()).isEqualTo(10);
		assertThat(gb.getGenericListProperty().get(0).iterator().next()).isEqualTo(20);
		assertThat(gb.getGenericListProperty().get(1).iterator().next()).isEqualTo(30);
	}

	@Test
	void testSettingGenericPropertyWithReadOnlyInterface() {
		Bar bar = new Bar();
		BeanWrapper bw = new BeanWrapperImpl(bar);
		bw.setPropertyValue("version", "10");
		assertThat(bar.getVersion()).isEqualTo(Double.valueOf(10.0));
	}

	@Test
	void testSettingLongPropertyWithGenericInterface() {
		Promotion bean = new Promotion();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.setPropertyValue("id", "10");
		assertThat(bean.getId()).isEqualTo(Long.valueOf(10));
	}

	@Test
	void testUntypedPropertyWithMapAtRuntime() {
		class Holder<D> {
			private final D data;
			public Holder(D data) {
				this.data = data;
			}
			@SuppressWarnings("unused")
			public D getData() {
				return this.data;
			}
		}

		Map<String, Object> data = new HashMap<>();
		data.put("x", "y");
		Holder<Map<String, Object>> context = new Holder<>(data);

		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(context);
		assertThat(bw.getPropertyValue("data['x']")).isEqualTo("y");

		bw.setPropertyValue("data['message']", "it works!");
		assertThat(data.get("message")).isEqualTo("it works!");
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

		private MultiValueMap<Integer, Long> multiValueMap = new LinkedMultiValueMap<>();

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

		public void setMultiValueMap(MultiValueMap<Integer, Long> multiValueMap) {
			this.multiValueMap = multiValueMap;
		}

		public MultiValueMap<Integer, Long> getMultiValueMap() {
			return multiValueMap;
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
