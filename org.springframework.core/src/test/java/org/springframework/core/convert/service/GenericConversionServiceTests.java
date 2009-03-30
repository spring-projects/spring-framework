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

import java.security.Principal;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionExecutionException;
import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.ConversionExecutorNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.TypedValue;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.NumberToNumber;
import org.springframework.core.convert.converter.StringToEnum;
import org.springframework.core.convert.converter.StringToInteger;

public class GenericConversionServiceTests extends TestCase {

	private GenericConversionService service = new GenericConversionService();

	private TypedValue value(Object obj) {
		return new TypedValue(obj, new TypeDescriptor(obj.getClass()));
	}
	
	private TypeDescriptor type(Class clazz) {
		return new TypeDescriptor(clazz);
	}
	
	public void testExecuteConversion() {
		service.addConverter(new StringToInteger());
		assertEquals(new Integer(3), service.executeConversion(value("3"), type(Integer.class)));
	}

	public void testConverterConversionForwardIndex() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(type(String.class), type(Integer.class));
		Integer three = (Integer) executor.execute("3");
		assertEquals(3, three.intValue());
	}

	public void testConverterConversionReverseIndex() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(type(Integer.class), type(String.class));
		String threeString = (String) executor.execute(new Integer(3));
		assertEquals("3", threeString);
	}

	public void testConversionExecutorNotFound() {
		try {
			service.getConversionExecutor(type(String.class), type(Integer.class));
			fail("Should have thrown an exception");
		} catch (ConversionExecutorNotFoundException e) {
		}
	}

	public void testConversionCompatibleTypes() {
		String source = "foo";
		assertSame(source, service.getConversionExecutor(type(String.class), type(String.class)).execute(source));
	}

	public void testConversionExecutorNullArgument() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(type(String.class), type(Integer.class));
		assertNull(executor.execute(null));
	}

	public void testConversionExecutorWrongTypeArgument() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(type(Integer.class), type(String.class));
		try {
			executor.execute("BOGUS");
			fail("Should have failed");
		} catch (ConversionExecutionException e) {

		}
	}

	public void testConverterConversionSuperSourceType() {
		service.addConverter(new Converter<CharSequence, Integer>() {
			public Integer convert(CharSequence source) throws Exception {
				return Integer.valueOf(source.toString());
			}

			public CharSequence convertBack(Integer target) throws Exception {
				return target.toString();
			}
		});
		ConversionExecutor executor = service.getConversionExecutor(type(String.class), type(Integer.class));
		Integer result = (Integer) executor.execute("3");
		assertEquals(new Integer(3), result);
	}

	public void testConverterConversionNoSuperTargetType() {
		service.addConverter(new Converter<CharSequence, Number>() {
			public Integer convert(CharSequence source) throws Exception {
				return Integer.valueOf(source.toString());
			}

			public CharSequence convertBack(Number target) throws Exception {
				return target.toString();
			}
		});
		try {
			ConversionExecutor executor = service.getConversionExecutor(type(String.class), type(Integer.class));
			fail("Should have failed");
		} catch (ConversionExecutorNotFoundException e) {

		}
	}

	public void testConversionObjectToPrimitive() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(type(String.class), type(int.class));
		Integer three = (Integer) executor.execute("3");
		assertEquals(3, three.intValue());
	}

	public void testConversionArrayToArray() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(type(String[].class), type(Integer[].class));
		Integer[] result = (Integer[]) executor.execute(new String[] { "1", "2", "3" });
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	public void testConversionArrayToPrimitiveArray() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(type(String[].class), type(int[].class));
		int[] result = (int[]) executor.execute(new String[] { "1", "2", "3" });
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	public void testConversionArrayToListInterface() {
		ConversionExecutor executor = service.getConversionExecutor(type(String[].class), type(List.class));
		List result = (List) executor.execute(new String[] { "1", "2", "3" });
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	public void testConversionArrayToListGenericTypeConversion() {
		ConversionExecutor executor = service.getConversionExecutor(type(String[].class), type(List.class));
		List result = (List) executor.execute(new String[] { "1", "2", "3" });
		assertEquals(new Integer("1"), result.get(0));
		assertEquals(new Integer("2"), result.get(1));
		assertEquals(new Integer("3"), result.get(2));
	}
	
	public void testConversionArrayToListImpl() {
		ConversionExecutor executor = service.getConversionExecutor(type(String[].class), type(LinkedList.class));
		LinkedList result = (LinkedList) executor.execute(new String[] { "1", "2", "3" });
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	public void testConversionArrayToAbstractList() {
		try {
			service.getConversionExecutor(type(String[].class), type(AbstractList.class));
		} catch (IllegalArgumentException e) {

		}
	}

	public void testConversionListToArray() {
		ConversionExecutor executor = service.getConversionExecutor(type(Collection.class), type(String[].class));
		List list = new ArrayList();
		list.add("1");
		list.add("2");
		list.add("3");
		String[] result = (String[]) executor.execute(list);
		assertEquals("1", result[0]);
		assertEquals("2", result[1]);
		assertEquals("3", result[2]);
	}

	public void testConversionListToArrayWithComponentConversion() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(type(Collection.class), type(Integer[].class));
		List list = new ArrayList();
		list.add("1");
		list.add("2");
		list.add("3");
		Integer[] result = (Integer[]) executor.execute(list);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	public void testConversionObjectToArray() {
		ConversionExecutor executor = service.getConversionExecutor(type(String.class), type(String[].class));
		String[] result = (String[]) executor.execute("1,2,3");
		assertEquals(1, result.length);
		assertEquals("1,2,3", result[0]);
	}

	public void testConversionObjectToArrayWithElementConversion() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(type(String.class), type(Integer[].class));
		Integer[] result = (Integer[]) executor.execute("123");
		assertEquals(1, result.length);
		assertEquals(new Integer(123), result[0]);
	}

	public static enum FooEnum {
		BAR
	}

	public void testSuperConverterConversionForwardIndex() {
		service.addConverter(new StringToEnum());
		ConversionExecutor executor = service.getConversionExecutor(type(String.class), type(FooEnum.class));
		assertEquals(FooEnum.BAR, executor.execute("BAR"));
	}

	public void testSuperTwoWayConverterConversionReverseIndex() {
		service.addConverter(new StringToEnum());
		ConversionExecutor executor = service.getConversionExecutor(type(FooEnum.class), type(String.class));
		assertEquals("BAR", executor.execute(FooEnum.BAR));
	}

	public void testSuperConverterConversionNotConvertibleAbstractType() {
		service.addConverter(new StringToEnum());
		ConversionExecutor executor = service.getConversionExecutor(type(String.class), type(Enum.class));
		try {
			executor.execute("WHATEV");
			fail("Should have failed");
		} catch (ConversionExecutionException e) {

		}
	}

	public void testSuperConverterConversionNotConvertibleAbstractType2() {
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
		ConversionExecutor executor = service.getConversionExecutor(type(Integer.class), type(customNumber.getClass()));
		try {
			executor.execute(3);
			fail("Should have failed");
		} catch (ConversionExecutionException e) {

		}
	}

	public void testAddConverterNoSourceTargetClassInfoAvailable() {
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

	public void testCustomConverterConversionForwardIndex() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", type(String.class), type(Principal.class));
		assertEquals("keith", ((Principal) executor.execute("keith")).getName());
	}

	public void testCustomConverterConversionReverseIndex() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", type(Principal.class), type(String.class));
		assertEquals("keith", executor.execute(new Principal() {
			public String getName() {
				return "keith";
			}
		}));
	}

	public void testCustomConverterConversionForSameType() {
		service.addConverter("trimmer", new Trimmer());
		ConversionExecutor executor = service.getConversionExecutor("trimmer", type(String.class), type(String.class));
		assertEquals("a string", executor.execute("a string   "));
	}

	public void testCustomConverterLookupNotCompatibleSource() {
		service.addConverter("trimmer", new Trimmer());
		try {
			service.getConversionExecutor("trimmer", type(Object.class), type(String.class));
			fail("Should have failed");
		} catch (ConversionException e) {

		}
	}

	public void testCustomConverterLookupNotCompatibleTarget() {
		service.addConverter("trimmer", new Trimmer());
		try {
			service.getConversionExecutor("trimmer", type(String.class), type(Object.class));
		} catch (ConversionException e) {

		}
	}

	public void testCustomConverterLookupNotCompatibleTargetReverse() {
		service.addConverter("princy", new CustomTwoWayConverter());
		try {
			service.getConversionExecutor("princy", type(Principal.class), type(Integer.class));
		} catch (ConversionException e) {

		}
	}

	public void testCustomConverterConversionArrayToArray() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", type(String[].class), type(Principal[].class));
		Principal[] p = (Principal[]) executor.execute(new String[] { "princy1", "princy2" });
		assertEquals("princy1", p[0].getName());
		assertEquals("princy2", p[1].getName());
	}

	public void testCustomConverterConversionArrayToArrayReverse() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", type(Principal[].class), type(String[].class));
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

	public void testCustomConverterLookupArrayToArrayBogusSource() {
		service.addConverter("princy", new CustomTwoWayConverter());
		try {
			service.getConversionExecutor("princy", type(Integer[].class), type(Principal[].class));
			fail("Should have failed");
		} catch (ConversionExecutorNotFoundException e) {
		}
	}

	public void testCustomConverterLookupArrayToArrayBogusTarget() {
		service.addConverter("princy", new CustomTwoWayConverter());
		try {
			service.getConversionExecutor("princy", type(Principal[].class), type(Integer[].class));
		} catch (ConversionExecutorNotFoundException e) {

		}
	}

	public void testCustomConverterConversionArrayToCollection() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", type(String[].class), type(List.class));
		List list = (List) executor.execute(new String[] { "princy1", "princy2" });
		assertEquals("princy1", ((Principal) list.get(0)).getName());
		assertEquals("princy2", ((Principal) list.get(1)).getName());
	}

	public void testCustomConverterConversionArrayToCollectionReverse() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", type(Principal[].class), type(List.class));
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

	public void testCustomConverterLookupArrayToCollectionBogusSource() {
		service.addConverter("princy", new CustomTwoWayConverter());
		try {
			service.getConversionExecutor("princy", type(Integer[].class), type(List.class));
			fail("Should have failed");
		} catch (ConversionExecutorNotFoundException e) {

		}
	}

	public void testCustomConverterLookupCollectionToArray() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", type(List.class), type(Principal[].class));
		List princyList = new ArrayList();
		princyList.add("princy1");
		princyList.add("princy2");
		Principal[] p = (Principal[]) executor.execute(princyList);
		assertEquals("princy1", p[0].getName());
		assertEquals("princy2", p[1].getName());
	}

	public void testCustomConverterLookupCollectionToArrayReverse() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", type(List.class), type(String[].class));
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

	public void testtestCustomConverterLookupCollectionToArrayBogusTarget() {
		service.addConverter("princy", new CustomTwoWayConverter());
		try {
			service.getConversionExecutor("princy", type(List.class), type(Integer[].class));
			fail("Should have failed");
		} catch (ConversionExecutorNotFoundException e) {

		}
	}

	public void testCustomConverterConversionObjectToArray() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", type(String.class), type(Principal[].class));
		Principal[] p = (Principal[]) executor.execute("princy1");
		assertEquals("princy1", p[0].getName());
	}

	public void testCustomConverterConversionObjectToArrayReverse() {
		service.addConverter("princy", new CustomTwoWayConverter());
		ConversionExecutor executor = service.getConversionExecutor("princy", type(Principal.class), type(String[].class));
		final Principal princy1 = new Principal() {
			public String getName() {
				return "princy1";
			}
		};
		String[] p = (String[]) executor.execute(princy1);
		assertEquals("princy1", p[0]);
	}

	public void testCustomConverterLookupObjectToArrayBogusSource() {
		service.addConverter("princy", new CustomTwoWayConverter());
		try {
			service.getConversionExecutor("princy", type(Integer.class), type(Principal[].class));
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
		// this fails at the moment
		//service.addConverter(GenericConversionService.converterFor(String.class, FooEnum.class, new StringToEnum()));
		//assertEquals(FooEnum.BAR, service.executeConversion("BAR", FooEnum.class));
	}

}