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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
		converter.add(new StringToInteger());
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
			converter.add(new Converter() {
				public Object convert(Object source) throws Exception {
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
	
	@Test
	public void convertNullTargetClass() {
		assertEquals("3", converter.convert("3", (Class<?>)null));
	}

	@Test
	public void convertNullConversionPointType() {
		assertEquals("3", converter.convert("3", TypeDescriptor.NULL));
	}

	
	@Test
	public void convertWrongTypeArgument() {
		converter.add(new StringToInteger());
		try {
			converter.convert("BOGUS", Integer.class);
			fail("Should have failed");
		} catch (ConversionFailedException e) {

		}
	}

	@Test
	public void convertSuperSourceType() {
		converter.add(new Converter<CharSequence, Integer>() {
			public Integer convert(CharSequence source) throws Exception {
				return Integer.valueOf(source.toString());
			}
		});
		Integer result = converter.convert("3", Integer.class);
		assertEquals(new Integer(3), result);
	}

	@Test
	public void convertObjectToPrimitive() {
		converter.add(new StringToInteger());
		Integer three = converter.convert("3", int.class);
		assertEquals(3, three.intValue());
	}

	@Test
	public void convertArrayToArray() {
		converter.add(new StringToInteger());
		Integer[] result = converter.convert(new String[] { "1", "2", "3" }, Integer[].class);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	@Test
	public void convertArrayToPrimitiveArray() {
		converter.add(new StringToInteger());
		int[] result = converter.convert(new String[] { "1", "2", "3" }, int[].class);
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
		converter.add(new StringToInteger());
		List<Integer> result = (List<Integer>) converter.convert(new String[] { "1", "2", "3" }, new TypeDescriptor(getClass().getDeclaredField("genericList")));
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

	@Test
	public void convertArrayToAbstractList() {
		try {
			converter.convert(new String[] { "1", "2", "3" }, AbstractList.class);
		} catch (IllegalArgumentException e) {

		}
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
		converter.add(new StringToInteger());
		List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		Integer[] result = converter.convert(list, Integer[].class);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	public Map<Integer, FooEnum> genericMap = new HashMap<Integer, FooEnum>();

	@Test
	public void convertMapToMap() throws Exception {
		Map<String, String> foo = new HashMap<String, String>();
		foo.put("1", "BAR");
		foo.put("2", "BAZ");
		converter.add(new StringToInteger());
		converter.add(new StringToEnumFactory().getConverter(FooEnum.class));
		Map<String, FooEnum> map = (Map<String, FooEnum>) converter.convert(foo, new TypeDescriptor(getClass().getField("genericMap")));
		assertEquals(map.get(1), FooEnum.BAR);
		assertEquals(map.get(2), FooEnum.BAZ);
	}

	@Test
	public void convertStringToArray() {
		String[] result = (String[]) converter.convert("1,2,3", String[].class);
		assertEquals(3, result.length);
		assertEquals("1", result[0]);
		assertEquals("2", result[1]);
		assertEquals("3", result[2]);
	}
	
	@Test
	public void convertStringToArrayWithElementConversion() {
		converter.add(new StringToInteger());		
		Integer[] result = converter.convert("1,2,3", Integer[].class);
		assertEquals(3, result.length);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}


	public static enum FooEnum {

		BAR, BAZ
	}

}
