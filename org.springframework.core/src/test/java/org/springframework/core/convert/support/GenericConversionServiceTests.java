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

package org.springframework.core.convert.support;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Keith Donald
 */
public class GenericConversionServiceTests {

	private GenericConversionService converter = new GenericConversionService();

	@Test
	public void executeConversion() {
		converter.addConverterFactory(new StringToNumberConverterFactory());
		assertEquals(new Integer(3), converter.convert("3", Integer.class));
	}

	@Test
	public void executeConversionNullSource() {
		assertEquals(null, converter.convert(null, Integer.class));
	}

	@Test
	public void executeCompatibleSource() {
		assertEquals(Boolean.FALSE, converter.convert(false, boolean.class));
	}

	@Test
	public void converterNotFound() {
		try {
			converter.convert("3", Integer.class);
			fail("Should have thrown an exception");
		} catch (ConverterNotFoundException e) {
		}
	}

	@Test
	public void addConverterNoSourceTargetClassInfoAvailable() {
		try {
			converter.addConverter(new Converter() {
				public Object convert(Object source) {
					return source;
				}
			});
			fail("Should have failed");
		} catch (IllegalArgumentException e) {

		}
	}

	@Test
	public void convertNull() {
		assertNull(converter.convert(null, Integer.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void convertNullTargetClass() {
		assertEquals("3", converter.convert("3", (Class<?>) null));
	}

	@Test
	public void convertNullConversionPointType() {
		assertEquals(null, converter.convert(3, TypeDescriptor.valueOf(String.class), TypeDescriptor.NULL));
	}

	@Test
	public void convertWrongTypeArgument() {
		converter.addConverterFactory(new StringToNumberConverterFactory());
		try {
			converter.convert("BOGUS", Integer.class);
			fail("Should have failed");
		} catch (ConversionFailedException e) {

		}
	}

	@Test
	public void convertSuperSourceType() {
		converter.addConverter(new Converter<CharSequence, Integer>() {
			public Integer convert(CharSequence source) {
				return Integer.valueOf(source.toString());
			}
		});
		Integer result = converter.convert("3", Integer.class);
		assertEquals(new Integer(3), result);
	}

	@Test
	public void convertObjectToPrimitive() {
		converter.addConverterFactory(new StringToNumberConverterFactory());
		Integer three = converter.convert("3", int.class);
		assertEquals(3, three.intValue());
	}

	@Test
	public void convertArrayToArray() {
		converter.addConverterFactory(new StringToNumberConverterFactory());
		Integer[] result = converter.convert(new String[] { "1", "2", "3" }, Integer[].class);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	@Test
	public void convertArrayToPrimitiveArray() {
		converter.addConverterFactory(new StringToNumberConverterFactory());
		int[] result = converter.convert(new String[] { "1", "2", "3" }, int[].class);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test
	public void convertArrayToArrayAssignable() {
		int[] result = converter.convert(new int[] { 1, 2, 3 }, int[].class);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test
	public void convertArrayToListInterface() {
		List<?> result = converter.convert(new String[] { "1", "2", "3" }, List.class);
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	public List<Integer> genericList = new ArrayList<Integer>();

	@Test
	public void convertArrayToListGenericTypeConversion() throws Exception {
		converter.addConverterFactory(new StringToNumberConverterFactory());
		List<Integer> result = (List<Integer>) converter.convert(new String[] { "1", "2", "3" }, TypeDescriptor
				.valueOf(String[].class), new TypeDescriptor(getClass().getDeclaredField("genericList")));
		assertEquals(new Integer("1"), result.get(0));
		assertEquals(new Integer("2"), result.get(1));
		assertEquals(new Integer("3"), result.get(2));
	}

	@Test
	public void convertArrayToListImpl() {
		LinkedList<?> result = converter.convert(new String[] { "1", "2", "3" }, LinkedList.class);
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	@Test(expected = ConversionFailedException.class)
	public void convertArrayToAbstractList() {
		converter.convert(new String[] { "1", "2", "3" }, AbstractList.class);
	}

	@Test
	public void convertListToArray() {
		List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		String[] result = converter.convert(list, String[].class);
		assertEquals("1", result[0]);
		assertEquals("2", result[1]);
		assertEquals("3", result[2]);
	}

	@Test
	public void convertListToArrayWithComponentConversion() {
		converter.addConverterFactory(new StringToNumberConverterFactory());
		List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		Integer[] result = converter.convert(list, Integer[].class);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	@Test
	public void convertCollectionToCollection() throws Exception {
		converter.addConverterFactory(new StringToNumberConverterFactory());
		Set<String> foo = new LinkedHashSet<String>();
		foo.add("1");
		foo.add("2");
		foo.add("3");
		List<Integer> bar = (List<Integer>) converter.convert(foo, TypeDescriptor.valueOf(List.class),
				new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(new Integer(1), bar.get(0));
		assertEquals(new Integer(2), bar.get(1));
		assertEquals(new Integer(3), bar.get(2));
	}

	public Map<Integer, FooEnum> genericMap = new HashMap<Integer, FooEnum>();

	@Test
	public void convertMapToMap() throws Exception {
		Map<String, String> foo = new HashMap<String, String>();
		foo.put("1", "BAR");
		foo.put("2", "BAZ");
		converter.addConverterFactory(new StringToNumberConverterFactory());
		converter.addConverterFactory(new StringToEnumConverterFactory());
		Map<String, FooEnum> map = (Map<String, FooEnum>) converter.convert(foo, TypeDescriptor.valueOf(Map.class),
				new TypeDescriptor(getClass().getField("genericMap")));
		assertEquals(map.get(1), FooEnum.BAR);
		assertEquals(map.get(2), FooEnum.BAZ);
	}

	@Test
	public void convertObjectToCollection() {
		List<String> result = (List<String>) converter.convert("test", List.class);
		assertEquals(1, result.size());
		assertEquals("test", result.get(0));
	}

	@Test
	public void convertObjectToCollectionWithElementConversion() throws Exception {
		converter.addConverterFactory(new StringToNumberConverterFactory());		
		List<Integer> result = (List<Integer>) converter.convert("3", TypeDescriptor.valueOf(String.class),
				new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(1, result.size());
		assertEquals(new Integer(3), result.get(0));
	}

	@Test
	public void convertCollectionToObject() {
		List<String> list = Collections.singletonList("test");
		String result = converter.convert(list, String.class);
		assertEquals("test", result);
	}

	@Test
	public void convertCollectionToObjectWithElementConversion() {
		converter.addConverterFactory(new StringToNumberConverterFactory());		
		List<String> list = Collections.singletonList("3");		
		Integer result = converter.convert(list, Integer.class);
		assertEquals(new Integer(3), result);
	}

	@Test
	public void convertObjectToArray() {
		String[] result = converter.convert("test", String[].class);
		assertEquals(1, result.length);
		assertEquals("test", result[0]);
	}

	@Test
	public void convertObjectToArrayWithElementConversion() {
		converter.addConverterFactory(new StringToNumberConverterFactory());
		Integer[] result = converter.convert("1", Integer[].class);
		assertEquals(1, result.length);
		assertEquals(new Integer(1), result[0]);
	}

	@Test
	public void convertArrayToObject() {
		String[] array = new String[] { "test" };
		String result = converter.convert(array, String.class);
		assertEquals("test", result);
	}
	
	@Test
	public void convertArrayToObjectWithElementConversion() {
		converter.addConverterFactory(new StringToNumberConverterFactory());
		String[] array = new String[] { "3" };
		Integer result = converter.convert(array, Integer.class);
		assertEquals(new Integer(3), result);
	}
	
	@Test
	@Ignore
	public void convertStringToArrayWithElementConversion() {
		converter.addConverterFactory(new StringToNumberConverterFactory());
		Integer[] result = converter.convert("1,2,3", Integer[].class);
		assertEquals(3, result.length);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	@Test
	public void genericConverterDelegatingBackToConversionServiceConverterNotFound() {
		try {
			converter.convert("1", Integer[].class);
		} catch (ConversionFailedException e) {
			assertTrue(e.getCause() instanceof ConverterNotFoundException);
		}
	}

	@Test
	public void parent() {
		GenericConversionService parent = new GenericConversionService();
		converter.setParent(parent);
		assertFalse(converter.canConvert(String.class, Integer.class));
		try {
			converter.convert("3", Integer.class);
		} catch (ConverterNotFoundException e) {

		}
	}

	public static enum FooEnum {
		BAR, BAZ
	}

}
