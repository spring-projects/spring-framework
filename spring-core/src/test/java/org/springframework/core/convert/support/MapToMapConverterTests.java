/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.convert.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class MapToMapConverterTests {

	private GenericConversionService conversionService = new GenericConversionService();


	@Before
	public void setUp() {
		conversionService.addConverter(new MapToMapConverter(conversionService));
	}


	@Test
	public void scalarMap() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("1", "9");
		map.put("2", "37");
		TypeDescriptor sourceType = TypeDescriptor.forObject(map);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("scalarMapTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		try {
			conversionService.convert(map, sourceType, targetType);
		} catch (ConversionFailedException e) {
			assertTrue(e.getCause() instanceof ConverterNotFoundException);
		}
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")
		Map<Integer, Integer> result = (Map<Integer, Integer>) conversionService.convert(map, sourceType, targetType);
		assertFalse(map.equals(result));
		assertEquals((Integer) 9, result.get(1));
		assertEquals((Integer) 37, result.get(2));
	}

	@Test
	public void scalarMapNotGenericTarget() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("1", "9");
		map.put("2", "37");
		assertTrue(conversionService.canConvert(Map.class, Map.class));
		assertSame(map, conversionService.convert(map, Map.class));
	}

	@Test
	public void scalarMapNotGenericSourceField() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("1", "9");
		map.put("2", "37");
		TypeDescriptor sourceType = new TypeDescriptor(getClass().getField("notGenericMapSource"));
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("scalarMapTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		try {
			conversionService.convert(map, sourceType, targetType);
		} catch (ConversionFailedException e) {
			assertTrue(e.getCause() instanceof ConverterNotFoundException);
		}
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")
		Map<Integer, Integer> result = (Map<Integer, Integer>) conversionService.convert(map, sourceType, targetType);
		assertFalse(map.equals(result));
		assertEquals((Integer) 9, result.get(1));
		assertEquals((Integer) 37, result.get(2));
	}

	@Test
	public void collectionMap() throws Exception {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		map.put("1", Arrays.asList("9", "12"));
		map.put("2", Arrays.asList("37", "23"));
		TypeDescriptor sourceType = TypeDescriptor.forObject(map);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("collectionMapTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		try {
			conversionService.convert(map, sourceType, targetType);
		} catch (ConversionFailedException e) {
			assertTrue(e.getCause() instanceof ConverterNotFoundException);
		}
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")
		Map<Integer, List<Integer>> result = (Map<Integer, List<Integer>>) conversionService.convert(map, sourceType, targetType);
		assertFalse(map.equals(result));
		assertEquals(Arrays.asList(9, 12), result.get(1));
		assertEquals(Arrays.asList(37, 23), result.get(2));
	}

	@Test
	public void collectionMapSourceTarget() throws Exception {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		map.put("1", Arrays.asList("9", "12"));
		map.put("2", Arrays.asList("37", "23"));
		TypeDescriptor sourceType = new TypeDescriptor(getClass().getField("sourceCollectionMapTarget"));
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("collectionMapTarget"));
		assertFalse(conversionService.canConvert(sourceType, targetType));
		try {
			conversionService.convert(map, sourceType, targetType);
			fail("Should have failed");
		}
		catch (ConverterNotFoundException ex) {
			// expected
		}
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")
		Map<Integer, List<Integer>> result = (Map<Integer, List<Integer>>) conversionService.convert(map, sourceType, targetType);
		assertFalse(map.equals(result));
		assertEquals(Arrays.asList(9, 12), result.get(1));
		assertEquals(Arrays.asList(37, 23), result.get(2));
	}

	@Test
	public void collectionMapNotGenericTarget() throws Exception {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		map.put("1", Arrays.asList("9", "12"));
		map.put("2", Arrays.asList("37", "23"));
		assertTrue(conversionService.canConvert(Map.class, Map.class));
		assertSame(map, conversionService.convert(map, Map.class));
	}

	@Test
	public void collectionMapNotGenericTargetCollectionToObjectInteraction() throws Exception {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		map.put("1", Arrays.asList("9", "12"));
		map.put("2", Arrays.asList("37", "23"));
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		assertTrue(conversionService.canConvert(Map.class, Map.class));
		assertSame(map, conversionService.convert(map, Map.class));
	}

	@Test
	public void emptyMap() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(map);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("emptyMapTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		assertSame(map, conversionService.convert(map, sourceType, targetType));
	}

	@Test
	public void emptyMapNoTargetGenericInfo() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		assertTrue(conversionService.canConvert(Map.class, Map.class));
		assertSame(map, conversionService.convert(map, Map.class));
	}

	@Test
	public void emptyMapDifferentTargetImplType() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(map);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("emptyMapDifferentTarget"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, String> result = (LinkedHashMap<String, String>) conversionService.convert(map, sourceType, targetType);
		assertEquals(map, result);
		assertEquals(LinkedHashMap.class, result.getClass());
	}

	@Test
	public void noDefaultConstructorCopyNotRequired() throws Exception {
		// SPR-9284
		NoDefaultConstructorMap<String, Integer> map = new NoDefaultConstructorMap<String,Integer>(
				Collections.<String, Integer> singletonMap("1", 1));
		TypeDescriptor sourceType = TypeDescriptor.map(NoDefaultConstructorMap.class,
				TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class));
		TypeDescriptor targetType = TypeDescriptor.map(NoDefaultConstructorMap.class,
				TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		@SuppressWarnings("unchecked")
		Map<String, Integer> result = (Map<String, Integer>) conversionService.convert(map, sourceType, targetType);
		assertEquals(map, result);
		assertEquals(NoDefaultConstructorMap.class, result.getClass());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void multiValueMapToMultiValueMap() throws Exception {
		DefaultConversionService.addDefaultConverters(conversionService);
		MultiValueMap<String, Integer> source = new LinkedMultiValueMap<String, Integer>();
		source.put("a", Arrays.asList(1, 2, 3));
		source.put("b", Arrays.asList(4, 5, 6));
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("multiValueMapTarget"));
		MultiValueMap<String, String> converted = (MultiValueMap<String, String>) conversionService.convert(source, targetType);
		assertThat(converted.size(), equalTo(2));
		assertThat(converted.get("a"), equalTo(Arrays.asList("1", "2", "3")));
		assertThat(converted.get("b"), equalTo(Arrays.asList("4", "5", "6")));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void mapToMultiValueMap() throws Exception {
		DefaultConversionService.addDefaultConverters(conversionService);
		Map<String, Integer> source = new HashMap<String, Integer>();
		source.put("a", 1);
		source.put("b", 2);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("multiValueMapTarget"));
		MultiValueMap<String, String> converted = (MultiValueMap<String, String>) conversionService.convert(source, targetType);
		assertThat(converted.size(), equalTo(2));
		assertThat(converted.get("a"), equalTo(Arrays.asList("1")));
		assertThat(converted.get("b"), equalTo(Arrays.asList("2")));
	}

	@Test
	public void testStringToEnumMap() throws Exception {
		conversionService.addConverterFactory(new StringToEnumConverterFactory());
		Map<String, Integer> source = new HashMap<String, Integer>();
		source.put("A", 1);
		source.put("C", 2);
		EnumMap<MyEnum, Integer> result = new EnumMap<MyEnum, Integer>(MyEnum.class);
		result.put(MyEnum.A, 1);
		result.put(MyEnum.C, 2);
		assertEquals(result,
				conversionService.convert(source, TypeDescriptor.forObject(source), new TypeDescriptor(getClass().getField("enumMap"))));
	}


	@SuppressWarnings("serial")
	public static class NoDefaultConstructorMap<K, V> extends HashMap<K, V> {

		public NoDefaultConstructorMap(Map<? extends K, ? extends V> map) {
			super(map);
		}
	}


	public static enum MyEnum {A, B, C}


	public Map<Integer, Integer> scalarMapTarget;

	public Map<Integer, List<Integer>> collectionMapTarget;

	public Map<String, List<String>> sourceCollectionMapTarget;

	public Map<String, String> emptyMapTarget;

	public LinkedHashMap<String, String> emptyMapDifferentTarget;

	public MultiValueMap<String, String> multiValueMapTarget;

	@SuppressWarnings("rawtypes")
	public Map notGenericMapSource;

	public EnumMap<MyEnum, Integer> enumMap;

}
