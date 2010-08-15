/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class GenericConversionServiceTests {

	private GenericConversionService conversionService = new GenericConversionService();

	@Test
	public void convert() {
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertEquals(new Integer(3), conversionService.convert("3", Integer.class));
	}

	@Test
	public void convertNullSource() {
		assertEquals(null, conversionService.convert(null, Integer.class));
	}

	@Test
	public void convertAssignableSource() {
		assertEquals(Boolean.FALSE, conversionService.convert(false, boolean.class));
		assertEquals(Boolean.FALSE, conversionService.convert(false, Boolean.class));
	}

	@Test
	public void converterNotFound() {
		try {
			conversionService.convert("3", Integer.class);
			fail("Should have thrown an exception");
		}
		catch (ConverterNotFoundException e) {
		}
	}

	@Test
	public void addConverterNoSourceTargetClassInfoAvailable() {
		try {
			conversionService.addConverter(new Converter() {
				public Object convert(Object source) {
					return source;
				}
			});
			fail("Should have failed");
		} catch (IllegalArgumentException e) {

		}
	}

	@Test
	public void sourceTypeIsVoid() {
		GenericConversionService conversionService = new GenericConversionService();
		assertFalse(conversionService.canConvert(void.class, String.class));
	}

	@Test
	public void targetTypeIsVoid() {
		GenericConversionService conversionService = new GenericConversionService();
		assertFalse(conversionService.canConvert(String.class, void.class));
	}

	@Test
	public void convertNull() {
		assertNull(conversionService.convert(null, Integer.class));
	}

	public void convertNullTargetClass() {
		assertNull(conversionService.convert("3", null));
	}

	@Test
	public void convertNullTypeDescriptor() {
		assertNull(conversionService.convert("3", TypeDescriptor.valueOf(String.class), TypeDescriptor.NULL));
	}

	@Test
	public void convertWrongTypeArgument() {
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		try {
			conversionService.convert("BOGUS", Integer.class);
			fail("Should have failed");
		}
		catch (ConversionFailedException e) {

		}
	}

	@Test
	public void convertSuperSourceType() {
		conversionService.addConverter(new Converter<CharSequence, Integer>() {
			public Integer convert(CharSequence source) {
				return Integer.valueOf(source.toString());
			}
		});
		Integer result = conversionService.convert("3", Integer.class);
		assertEquals(new Integer(3), result);
	}

	@Test
	public void convertObjectToPrimitive() {
		assertFalse(conversionService.canConvert(String.class, boolean.class));
		conversionService.addConverter(new StringToBooleanConverter());
		assertTrue(conversionService.canConvert(String.class, boolean.class));
		Boolean b = conversionService.convert("true", boolean.class);
		assertEquals(Boolean.TRUE, b);
		assertTrue(conversionService.canConvert(TypeDescriptor.valueOf(String.class), TypeDescriptor
				.valueOf(boolean.class)));
		b = (Boolean) conversionService.convert("true", TypeDescriptor.valueOf(String.class), TypeDescriptor
				.valueOf(boolean.class));
		assertEquals(Boolean.TRUE, b);
	}

	@Test
	public void convertObjectToPrimitiveViaConverterFactory() {
		assertFalse(conversionService.canConvert(String.class, int.class));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertTrue(conversionService.canConvert(String.class, int.class));		
		Integer three = conversionService.convert("3", int.class);
		assertEquals(3, three.intValue());
	}
	
	@Test
	public void genericConverterDelegatingBackToConversionServiceConverterNotFound() {
		conversionService.addConverter(new ObjectToArrayConverter(conversionService));
		assertFalse(conversionService.canConvert(String.class, Integer[].class));
	}

	@Test
	public void testListToIterableConversion() {
		GenericConversionService conversionService = new GenericConversionService();
		List<Object> raw = new ArrayList<Object>();
		raw.add("one");
		raw.add("two");
		Object converted = conversionService.convert(raw, Iterable.class);
		assertSame(raw, converted);
	}

	@Test
	public void testListToObjectConversion() {
		GenericConversionService conversionService = new GenericConversionService();
		List<Object> raw = new ArrayList<Object>();
		raw.add("one");
		raw.add("two");
		Object converted = conversionService.convert(raw, Object.class);
		assertSame(raw, converted);
	}

	@Test
	public void testMapToObjectConversion() {
		GenericConversionService conversionService = new GenericConversionService();
		Map<Object, Object> raw = new HashMap<Object, Object>();
		raw.put("key", "value");
		Object converted = conversionService.convert(raw, Object.class);
		assertSame(raw, converted);
	}

	@Test
	public void testInterfaceToString() {
		GenericConversionService conversionService = new GenericConversionService();
		conversionService.addConverter(new MyBaseInterfaceConverter());
		conversionService.addConverter(new ObjectToStringConverter());
		Object converted = conversionService.convert(new MyInterfaceImplementer(), String.class);
		assertEquals("RESULT", converted);
	}

	@Test
	public void testInterfaceArrayToStringArray() {
		GenericConversionService conversionService = new GenericConversionService();
		conversionService.addConverter(new MyBaseInterfaceConverter());
		conversionService.addConverter(new ArrayToArrayConverter(conversionService));
		String[] converted = conversionService.convert(new MyInterface[] {new MyInterfaceImplementer()}, String[].class);
		assertEquals("RESULT", converted[0]);
	}

	@Test
	public void testObjectArrayToStringArray() {
		GenericConversionService conversionService = new GenericConversionService();
		conversionService.addConverter(new MyBaseInterfaceConverter());
		conversionService.addConverter(new ArrayToArrayConverter(conversionService));
		String[] converted = conversionService.convert(new MyInterfaceImplementer[] {new MyInterfaceImplementer()}, String[].class);
		assertEquals("RESULT", converted[0]);
	}

	@Test
	public void testStringArrayToResourceArray() {
		GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		conversionService.addConverter(new MyStringArrayToResourceArrayConverter());
		Resource[] converted = conversionService.convert(new String[] {"x1", "z3"}, Resource[].class);
		assertEquals(2, converted.length);
		assertEquals("1", converted[0].getDescription());
		assertEquals("3", converted[1].getDescription());
	}

	@Test
	public void testStringArrayToIntegerArray() {
		GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		conversionService.addConverter(new MyStringArrayToIntegerArrayConverter());
		Integer[] converted = conversionService.convert(new String[] {"x1", "z3"}, Integer[].class);
		assertEquals(2, converted.length);
		assertEquals(1, converted[0].intValue());
		assertEquals(3, converted[1].intValue());
	}

	@Test
	public void testStringToIntegerArray() {
		GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		conversionService.addConverter(new MyStringToIntegerArrayConverter());
		Integer[] converted = conversionService.convert("x1,z3", Integer[].class);
		assertEquals(2, converted.length);
		assertEquals(1, converted[0].intValue());
		assertEquals(3, converted[1].intValue());
	}

	@Test
	public void testWildcardMap() throws Exception {
		GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		Map<String, String> input = new LinkedHashMap<String, String>();
		input.put("key", "value");
		Object converted = conversionService.convert(input, TypeDescriptor.forObject(input),
				new TypeDescriptor(getClass().getField("wildcardMap")));
		assertEquals(input, converted);
	}

	@Test
	public void testListOfList() {
		GenericConversionService service = ConversionServiceFactory.createDefaultConversionService();
		List<List<String>> list = Collections.singletonList(Collections.singletonList("Foo"));
		assertNotNull(service.convert(list, String.class));
	}

	@Test
	public void testEmptyList() {
		GenericConversionService service = ConversionServiceFactory.createDefaultConversionService();
		List list = Collections.emptyList();
		List result = service.convert(list, List.class);
		assertSame(list, result);
		result = service.convert(list, list.getClass());
		assertSame(list, result);
	}

	@Test
	public void testEmptyMap() {
		GenericConversionService service = ConversionServiceFactory.createDefaultConversionService();
		Map map = Collections.emptyMap();
		Map result = service.convert(map, Map.class);
		assertSame(map, result);
		result = service.convert(map, map.getClass());
		assertSame(map, result);
	}

	@Test
	public void testStringToString() {
		GenericConversionService service = ConversionServiceFactory.createDefaultConversionService();
		String value = "myValue";
		String result = service.convert(value, String.class);
		assertSame(value, result);
	}

	@Test
	public void testStringToObject() {
		GenericConversionService service = ConversionServiceFactory.createDefaultConversionService();
		String value = "myValue";
		Object result = service.convert(value, Object.class);
		assertSame(value, result);
	}

	@Test
	public void testIgnoreCopyConstructor() {
		GenericConversionService service = ConversionServiceFactory.createDefaultConversionService();
		WithCopyConstructor value = new WithCopyConstructor();
		Object result = service.convert(value, WithCopyConstructor.class);
		assertSame(value, result);
	}

	@Test
	public void testPerformance1() {
		GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		StopWatch watch = new StopWatch("integer->string conversionPerformance");
		watch.start("convert 4,000,000 with conversion service");
		for (int i = 0; i < 4000000; i++) {
			conversionService.convert(3, String.class);
		}
		watch.stop();
		watch.start("convert 4,000,000 manually");
		for (int i = 0; i < 4000000; i++) {
			new Integer(3).toString();
		}
		watch.stop();
		System.out.println(watch.prettyPrint());
	}
	
	@Test
	public void testPerformance2() throws Exception {
		GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		StopWatch watch = new StopWatch("list<string> -> list<integer> conversionPerformance");
		watch.start("convert 4,000,000 with conversion service");
		List<String> source = new LinkedList<String>();
		source.add("1");
		source.add("2");
		source.add("3");
		TypeDescriptor td = new TypeDescriptor(getClass().getField("list"));
		for (int i = 0; i < 1000000; i++) {
			conversionService.convert(source, TypeDescriptor.forObject(source), td);
		}
		watch.stop();
		watch.start("convert 4,000,000 manually");
		for (int i = 0; i < 4000000; i++) {
			List<Integer> target = new ArrayList<Integer>(source.size());
			for (String element : source) {
				target.add(Integer.valueOf(element));
			}
		}
		watch.stop();		
		System.out.println(watch.prettyPrint());
	}

	public static List<Integer> list;

	@Test
	public void testPerformance3() throws Exception {
		GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		StopWatch watch = new StopWatch("map<string, string> -> map<string, integer> conversionPerformance");
		watch.start("convert 4,000,000 with conversion service");
		Map<String, String> source = new HashMap<String, String>();
		source.put("1", "1");
		source.put("2", "2");
		source.put("3", "3");
		TypeDescriptor td = new TypeDescriptor(getClass().getField("map"));		
		for (int i = 0; i < 1000000; i++) {
			conversionService.convert(source, TypeDescriptor.forObject(source), td);
		}
		watch.stop();
		watch.start("convert 4,000,000 manually");
		for (int i = 0; i < 4000000; i++) {
			Map<String, Integer> target = new HashMap<String, Integer>(source.size());
			for (Map.Entry<String, String> entry : source.entrySet()) {
				target.put(entry.getKey(), Integer.valueOf(entry.getValue()));
			}
		}
		watch.stop();		
		System.out.println(watch.prettyPrint());
	}
	
	public static Map<String, Integer> map;


	private interface MyBaseInterface {

	}


	private interface MyInterface extends MyBaseInterface {

	}


	private static class MyInterfaceImplementer implements MyInterface {

	}


	private static class MyBaseInterfaceConverter implements Converter<MyBaseInterface, String> {

		public String convert(MyBaseInterface source) {
			return "RESULT";
		}
	}


	private static class MyStringArrayToResourceArrayConverter implements Converter<String[], Resource[]>	{

		public Resource[] convert(String[] source) {
			Resource[] result = new Resource[source.length];
			for (int i = 0; i < source.length; i++) {
				result[i] = new DescriptiveResource(source[i].substring(1));
			}
			return result;
		}
	}


	private static class MyStringArrayToIntegerArrayConverter implements Converter<String[], Integer[]>	{

		public Integer[] convert(String[] source) {
			Integer[] result = new Integer[source.length];
			for (int i = 0; i < source.length; i++) {
				result[i] = Integer.parseInt(source[i].substring(1));
			}
			return result;
		}
	}


	private static class MyStringToIntegerArrayConverter implements Converter<String, Integer[]>	{

		public Integer[] convert(String source) {
			String[] srcArray = StringUtils.commaDelimitedListToStringArray(source);
			Integer[] result = new Integer[srcArray.length];
			for (int i = 0; i < srcArray.length; i++) {
				result[i] = Integer.parseInt(srcArray[i].substring(1));
			}
			return result;
		}
	}


	public static class WithCopyConstructor {

		public WithCopyConstructor() {
		}

		public WithCopyConstructor(WithCopyConstructor value) {
		}
	}


	public static Map<String, ?> wildcardMap;

}
