/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.core.convert.service;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.fail;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.convert.ConversionExecutionException;
import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.ConversionExecutorNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.NumberToNumber;
import org.springframework.core.convert.converter.StringToEnum;
import org.springframework.core.convert.converter.StringToInteger;

public class GenericConversionServiceTests {

	private GenericConversionService service = new GenericConversionService();

	@Test
	public void executeConversion() {
		service.addConverter(new StringToInteger());
		assertEquals(new Integer(3), service.executeConversion("3", type(Integer.class)));
	}

	@Test
	public void executeConversionNullSource() {
		assertEquals(null, service.executeConversion(null, type(Integer.class)));
	}
	
	@Test
	public void executeCompatibleSource() {
		assertEquals(false, service.executeConversion(false, type(boolean.class)));
	}

	@Test
	public void executeCompatibleSource2() {
		assertEquals(3, service.getConversionExecutor(Integer.class, TypeDescriptor.valueOf(int.class)).execute(new Integer(3)));
		assertEquals(3, service.getConversionExecutor(int.class, TypeDescriptor.valueOf(Integer.class)).execute(3));
	}
	
	@Test
	public void converterConvertForwardIndex() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String.class, type(Integer.class));
		Integer three = (Integer) executor.execute("3");
		assertEquals(3, three.intValue());
	}

	@Test
	public void convertReverseIndex() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(Integer.class, type(String.class));
		String threeString = (String) executor.execute(new Integer(3));
		assertEquals("3", threeString);
	}

	@Test
	public void convertExecutorNotFound() {
		try {
			service.getConversionExecutor(String.class, type(Integer.class));
			fail("Should have thrown an exception");
		} catch (ConversionExecutorNotFoundException e) {
		}
	}

	@Test
	public void addConverterNoSourceTargetClassInfoAvailable() {
		try {
			service.addConverter(new Converter() {
				public Object convert(Object source) throws Exception {
					return source;
				}

				public Object convertBack(Object target) throws Exception {
					return target;
				}
			});
			fail("Should have failed");
		} catch (IllegalArgumentException e) {

		}
	}

	@Test
	public void convertCompatibleTypes() {
		String source = "foo";
		assertSame(source, service.getConversionExecutor(String.class, type(String.class)).execute(source));
	}

	@Test
	public void convertNull() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String.class, type(Integer.class));
		assertNull(executor.execute(null));
	}

	@Test
	public void convertWrongTypeArgument() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(Integer.class, type(String.class));
		try {
			executor.execute("BOGUS");
			fail("Should have failed");
		} catch (ConversionExecutionException e) {

		}
	}

	@Test
	public void convertSuperSourceType() {
		service.addConverter(new Converter<CharSequence, Integer>() {
			public Integer convert(CharSequence source) throws Exception {
				return Integer.valueOf(source.toString());
			}

			public CharSequence convertBack(Integer target) throws Exception {
				return target.toString();
			}
		});
		ConversionExecutor executor = service.getConversionExecutor(String.class, type(Integer.class));
		Integer result = (Integer) executor.execute("3");
		assertEquals(new Integer(3), result);
	}

	@Test
	public void convertNoSuperTargetType() {
		service.addConverter(new Converter<CharSequence, Number>() {
			public Integer convert(CharSequence source) throws Exception {
				return Integer.valueOf(source.toString());
			}

			public CharSequence convertBack(Number target) throws Exception {
				return target.toString();
			}
		});
		try {
			ConversionExecutor executor = service.getConversionExecutor(String.class, type(Integer.class));
			fail("Should have failed");
		} catch (ConversionExecutorNotFoundException e) {

		}
	}

	@Test
	public void convertObjectToPrimitive() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String.class, type(int.class));
		Integer three = (Integer) executor.execute("3");
		assertEquals(3, three.intValue());
	}

	@Test
	public void convertArrayToArray() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String[].class, type(Integer[].class));
		Integer[] result = (Integer[]) executor.execute(new String[] { "1", "2", "3" });
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	@Test
	public void convertArrayToPrimitiveArray() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String[].class, type(int[].class));
		int[] result = (int[]) executor.execute(new String[] { "1", "2", "3" });
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test
	public void convertArrayToListInterface() {
		ConversionExecutor executor = service.getConversionExecutor(String[].class, type(List.class));
		List result = (List) executor.execute(new String[] { "1", "2", "3" });
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	public List<Integer> genericList = new ArrayList<Integer>();

	@Test
	public void convertArrayToListGenericTypeConversion() throws Exception {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String[].class, new TypeDescriptor(getClass()
				.getDeclaredField("genericList")));
		List result = (List) executor.execute(new String[] { "1", "2", "3" });
		assertEquals(new Integer("1"), result.get(0));
		assertEquals(new Integer("2"), result.get(1));
		assertEquals(new Integer("3"), result.get(2));
	}

	@Test
	public void convertArrayToListImpl() {
		ConversionExecutor executor = service.getConversionExecutor(String[].class, type(LinkedList.class));
		LinkedList result = (LinkedList) executor.execute(new String[] { "1", "2", "3" });
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	@Test
	public void convertArrayToAbstractList() {
		try {
			service.getConversionExecutor(String[].class, type(AbstractList.class));
		} catch (IllegalArgumentException e) {

		}
	}

	@Test
	public void convertListToArray() {
		ConversionExecutor executor = service.getConversionExecutor(Collection.class, type(String[].class));
		List list = new ArrayList();
		list.add("1");
		list.add("2");
		list.add("3");
		String[] result = (String[]) executor.execute(list);
		assertEquals("1", result[0]);
		assertEquals("2", result[1]);
		assertEquals("3", result[2]);
	}

	@Test
	public void convertListToArrayWithComponentConversion() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(Collection.class, type(Integer[].class));
		List list = new ArrayList();
		list.add("1");
		list.add("2");
		list.add("3");
		Integer[] result = (Integer[]) executor.execute(list);
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
		service.addConverter(new StringToInteger());
		service.addConverter(new StringToEnum());
		service.executeConversion(foo, new TypeDescriptor(getClass().getField("genericMap")));
	}

	@Ignore
	@Test
	public void convertObjectToArray() {
		ConversionExecutor executor = service.getConversionExecutor(String.class, type(String[].class));
		String[] result = (String[]) executor.execute("1,2,3");
		assertEquals(1, result.length);
		assertEquals("1,2,3", result[0]);
	}

	@Ignore
	@Test
	public void convertObjectToArrayWithElementConversion() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String.class, type(Integer[].class));
		Integer[] result = (Integer[]) executor.execute("123");
		assertEquals(1, result.length);
		assertEquals(new Integer(123), result[0]);
	}

	public static enum FooEnum {
		BAR, BAZ
	}

	@Test
	public void superConverterConvertForwardIndex() {
		service.addConverter(new StringToEnum());
		ConversionExecutor executor = service.getConversionExecutor(String.class, type(FooEnum.class));
		assertEquals(FooEnum.BAR, executor.execute("BAR"));
	}

	@Test
	public void superTwoWayConverterConvertReverseIndex() {
		service.addConverter(new StringToEnum());
		ConversionExecutor executor = service.getConversionExecutor(FooEnum.class, type(String.class));
		assertEquals("BAR", executor.execute(FooEnum.BAR));
	}

	@Test
	public void superConverterConvertNotConvertibleAbstractType() {
		service.addConverter(new StringToEnum());
		ConversionExecutor executor = service.getConversionExecutor(String.class, type(Enum.class));
		try {
			executor.execute("WHATEV");
			fail("Should have failed");
		} catch (ConversionExecutionException e) {

		}
	}

	@Test
	public void superConverterConvertNotConvertibleAbstractType2() {
		service.addConverter(new NumberToNumber());
		Number customNumber = new Number() {
			@Override
			public double doubleValue() {
				return 0;
			}

			@Override
			public float floatValue() {
				return 0;
			}

			@Override
			public int intValue() {
				return 0;
			}

			@Override
			public long longValue() {
				return 0;
			}
		};
		ConversionExecutor executor = service.getConversionExecutor(Integer.class, type(customNumber.getClass()));
		try {
			executor.execute(3);
			fail("Should have failed");
		} catch (ConversionExecutionException e) {

		}
	}

	@Test
	public void testSuperTwoWayConverterConverterAdaption() {
		service.addConverter(GenericConversionService.converterFor(String.class, FooEnum.class, new StringToEnum()));
		assertEquals(FooEnum.BAR, service.executeConversion("BAR", type(FooEnum.class)));
	}

	private TypeDescriptor type(Class<?> clazz) {
		return TypeDescriptor.valueOf(clazz);
	}

}