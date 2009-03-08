package org.springframework.core.convert.service;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.core.convert.ConversionExecutionException;
import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.ConversionExecutorNotFoundException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.NumberToNumber;
import org.springframework.core.convert.converter.StringToEnum;
import org.springframework.core.convert.converter.StringToInteger;
import org.springframework.core.convert.service.GenericConversionService;

public class GenericConversionServiceTests extends TestCase {

	private GenericConversionService service = new GenericConversionService();

	public void testExecuteConversion() {
		service.addConverter(new StringToInteger());
		assertEquals(new Integer(3), service.executeConversion("3", Integer.class));
	}

	public void testConverterConversionForwardIndex() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String.class, Integer.class);
		Integer three = (Integer) executor.execute("3");
		assertEquals(3, three.intValue());
	}

	public void testConverterConversionReverseIndex() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(Integer.class, String.class);
		String threeString = (String) executor.execute(new Integer(3));
		assertEquals("3", threeString);
	}

	public void testConversionExecutorNotFound() {
		try {
			service.getConversionExecutor(String.class, Integer.class);
			fail("Should have thrown an exception");
		} catch (ConversionExecutorNotFoundException e) {
		}
	}

	public void testConversionCompatibleTypes() {
		ArrayList source = new ArrayList();
		assertSame(source, service.getConversionExecutor(ArrayList.class, List.class).execute(source));
	}

	public void testConversionExecutorNullArgument() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String.class, Integer.class);
		assertNull(executor.execute(null));
	}

	public void testConversionExecutorWrongTypeArgument() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(Integer.class, String.class);
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
		ConversionExecutor executor = service.getConversionExecutor(String.class, Integer.class);
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
			ConversionExecutor executor = service.getConversionExecutor(String.class, Integer.class);
			fail("Should have failed");
		} catch (ConversionExecutorNotFoundException e) {

		}
	}

	public void testConversionObjectToPrimitive() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String.class, int.class);
		Integer three = (Integer) executor.execute("3");
		assertEquals(3, three.intValue());
	}

	public void testConversionArrayToArray() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String[].class, Integer[].class);
		Integer[] result = (Integer[]) executor.execute(new String[] { "1", "2", "3" });
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	public void testConversionArrayToPrimitiveArray() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String[].class, int[].class);
		int[] result = (int[]) executor.execute(new String[] { "1", "2", "3" });
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	public void testConversionArrayToListInterface() {
		ConversionExecutor executor = service.getConversionExecutor(String[].class, List.class);
		List result = (List) executor.execute(new String[] { "1", "2", "3" });
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	public void testConversionArrayToListImpl() {
		ConversionExecutor executor = service.getConversionExecutor(String[].class, LinkedList.class);
		LinkedList result = (LinkedList) executor.execute(new String[] { "1", "2", "3" });
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	public void testConversionArrayToAbstractList() {
		try {
			service.getConversionExecutor(String[].class, AbstractList.class);
		} catch (IllegalArgumentException e) {

		}
	}

	public void testConversionListToArray() {
		ConversionExecutor executor = service.getConversionExecutor(Collection.class, String[].class);
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
		ConversionExecutor executor = service.getConversionExecutor(Collection.class, Integer[].class);
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
		ConversionExecutor executor = service.getConversionExecutor(String.class, String[].class);
		String[] result = (String[]) executor.execute("1,2,3");
		assertEquals(1, result.length);
		assertEquals("1,2,3", result[0]);
	}

	public void testConversionObjectToArrayWithElementConversion() {
		service.addConverter(new StringToInteger());
		ConversionExecutor executor = service.getConversionExecutor(String.class, Integer[].class);
		Integer[] result = (Integer[]) executor.execute("123");
		assertEquals(1, result.length);
		assertEquals(new Integer(123), result[0]);
	}

	public static enum FooEnum {
		BAR
	}

	public void testSuperConverterConversionForwardIndex() {
		service.addConverter(new StringToEnum());
		ConversionExecutor executor = service.getConversionExecutor(String.class, FooEnum.class);
		assertEquals(FooEnum.BAR, executor.execute("BAR"));
	}

	public void testSuperTwoWayConverterConversionReverseIndex() {
		service.addConverter(new StringToEnum());
		ConversionExecutor executor = service.getConversionExecutor(FooEnum.class, String.class);
		assertEquals("BAR", executor.execute(FooEnum.BAR));
	}

	public void testSuperConverterConversionNotConvertibleAbstractType() {
		service.addConverter(new StringToEnum());
		ConversionExecutor executor = service.getConversionExecutor(String.class, Enum.class);
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
		ConversionExecutor executor = service.getConversionExecutor(Integer.class, customNumber.getClass());
		try {
			executor.execute(3);
			fail("Should have failed");
		} catch (ConversionExecutionException e) {

		}
	}
}