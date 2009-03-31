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

import java.security.Principal;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.convert.ConversionException;
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

	@Ignore
	@Test
	public void customConverterConvertForwardIndex() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", String.class, type(Principal.class));
		assertEquals("keith", ((Principal) executor.execute("keith")).getName());
	}

	@Ignore
	@Test
	public void customConverterConvertReverseIndex() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", Principal.class, type(String.class));
		assertEquals("keith", executor.execute(new Principal() {
			public String getName() {
				return "keith";
			}
		}));
	}

	@Ignore
	@Test
	public void customConverterConvertForSameType() {
		service.addConverter("trimmer", new Trimmer());
		ConversionExecutor executor = service.getConversionExecutor("trimmer", String.class, type(String.class));
		assertEquals("a string", executor.execute("a string   "));
	}

	@Ignore
	@Test
	public void customConverterLookupNotCompatibleSource() {
		service.addConverter("trimmer", new Trimmer());
		try {
			service.getConversionExecutor("trimmer", Object.class, type(String.class));
			fail("Should have failed");
		} catch (ConversionException e) {

		}
	}

	@Ignore
	@Test
	public void customConverterLookupNotCompatibleTarget() {
		service.addConverter("trimmer", new Trimmer());
		try {
			service.getConversionExecutor("trimmer", String.class, type(Object.class));
		} catch (ConversionException e) {

		}
	}

	@Ignore
	@Test
	public void customConverterLookupNotCompatibleTargetReverse() {
		service.addConverter("princy", new CustomTwoWayConverter());
		try {
			service.getConversionExecutor("princy", Principal.class, type(Integer.class));
		} catch (ConversionException e) {

		}
	}

	@Ignore
	@Test
	public void customConverterConvertArrayToArray() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", String[].class, type(Principal[].class));
		Principal[] p = (Principal[]) executor.execute(new String[] { "princy1", "princy2" });
		assertEquals("princy1", p[0].getName());
		assertEquals("princy2", p[1].getName());
	}

	@Ignore
	@Test
	public void customConverterConvertArrayToArrayReverse() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", Principal[].class, type(String[].class));
		final Principal princy1 = new Principal() {
			public String getName() {
				return "princy1";
			}
		};
		final Principal princy2 = new Principal() {
			public String getName() {
				return "princy2";
			}
		};
		String[] p = (String[]) executor.execute(new Principal[] { princy1, princy2 });
		assertEquals("princy1", p[0]);
		assertEquals("princy2", p[1]);
	}

	@Ignore
	@Test
	public void customConverterLookupArrayToArrayBogusSource() {
		service.addConverter("princy", new CustomTwoWayConverter());
		try {
			service.getConversionExecutor("princy", Integer[].class, type(Principal[].class));
			fail("Should have failed");
		} catch (ConversionExecutorNotFoundException e) {
		}
	}

	@Ignore
	@Test
	public void customConverterLookupArrayToArrayBogusTarget() {
		service.addConverter("princy", new CustomTwoWayConverter());
		try {
			service.getConversionExecutor("princy", Principal[].class, type(Integer[].class));
		} catch (ConversionExecutorNotFoundException e) {

		}
	}

	@Ignore
	@Test
	public void customConverterConvertArrayToCollection() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", String[].class, type(List.class));
		List list = (List) executor.execute(new String[] { "princy1", "princy2" });
		assertEquals("princy1", ((Principal) list.get(0)).getName());
		assertEquals("princy2", ((Principal) list.get(1)).getName());
	}

	@Ignore
	@Test
	public void customConverterConvertArrayToCollectionReverse() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", Principal[].class, type(List.class));
		final Principal princy1 = new Principal() {
			public String getName() {
				return "princy1";
			}
		};
		final Principal princy2 = new Principal() {
			public String getName() {
				return "princy2";
			}
		};
		List p = (List) executor.execute(new Principal[] { princy1, princy2 });
		assertEquals("princy1", p.get(0));
		assertEquals("princy2", p.get(1));
	}

	@Ignore
	@Test
	public void customConverterLookupArrayToCollectionBogusSource() {
		service.addConverter("princy", new CustomTwoWayConverter());
		try {
			service.getConversionExecutor("princy", Integer[].class, type(List.class));
			fail("Should have failed");
		} catch (ConversionExecutorNotFoundException e) {

		}
	}

	@Ignore
	@Test
	public void customConverterLookupCollectionToArray() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", List.class, type(Principal[].class));
		List princyList = new ArrayList();
		princyList.add("princy1");
		princyList.add("princy2");
		Principal[] p = (Principal[]) executor.execute(princyList);
		assertEquals("princy1", p[0].getName());
		assertEquals("princy2", p[1].getName());
	}

	@Ignore
	@Test
	public void customConverterLookupCollectionToArrayReverse() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", List.class, type(String[].class));
		final Principal princy1 = new Principal() {
			public String getName() {
				return "princy1";
			}
		};
		final Principal princy2 = new Principal() {
			public String getName() {
				return "princy2";
			}
		};
		List princyList = new ArrayList();
		princyList.add(princy1);
		princyList.add(princy2);
		String[] p = (String[]) executor.execute(princyList);
		assertEquals("princy1", p[0]);
		assertEquals("princy2", p[1]);
	}

	@Ignore
	@Test
	public void customConverterLookupCollectionToArrayBogusTarget() {
		service.addConverter("princy", new CustomTwoWayConverter());
		try {
			service.getConversionExecutor("princy", List.class, type(Integer[].class));
			fail("Should have failed");
		} catch (ConversionExecutorNotFoundException e) {

		}
	}

	@Ignore
	@Test
	public void customConverterConvertObjectToArray() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", String.class, type(Principal[].class));
		Principal[] p = (Principal[]) executor.execute("princy1");
		assertEquals("princy1", p[0].getName());
	}

	@Ignore
	@Test
	public void customConverterConvertObjectToArrayReverse() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", Principal.class, type(String[].class));
		final Principal princy1 = new Principal() {
			public String getName() {
				return "princy1";
			}
		};
		String[] p = (String[]) executor.execute(princy1);
		assertEquals("princy1", p[0]);
	}

	@Ignore
	@Test
	public void customConverterLookupObjectToArrayBogusSource() {
		service.addConverter("princy", new CustomTwoWayConverter());
		try {
			service.getConversionExecutor("princy", Integer.class, type(Principal[].class));
			fail("Should have failed");
		} catch (ConversionExecutorNotFoundException e) {

		}
	}

	private static class CustomTwoWayConverter implements Converter<String, Principal> {

		public Principal convert(final String source) throws Exception {
			return new Principal() {
				public String getName() {
					return (String) source;
				}
			};
		}

		public String convertBack(Principal target) throws Exception {
			return ((Principal) target).getName();
		}

	}

	private static class Trimmer implements Converter<String, String> {

		public String convert(String source) throws Exception {
			return ((String) source).trim();
		}

		public String convertBack(String target) throws Exception {
			throw new UnsupportedOperationException("Will never run");
		}

	}

	public void testSuperTwoWayConverterConverterAdaption() {
		service.addConverter(GenericConversionService.converterFor(String.class, FooEnum.class, new StringToEnum()));
		assertEquals(FooEnum.BAR, service.executeConversion("BAR", type(FooEnum.class)));
	}

	private TypeDescriptor type(Class<?> clazz) {
		return TypeDescriptor.valueOf(clazz);
	}

}