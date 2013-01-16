/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

public class NumberToObjectConversionTests {

	private enum TestEnum {
		ONE("1"), TWO("2"), MAX_INT(String.valueOf(Integer.MAX_VALUE));

		private String value;

		private TestEnum(String value) {
			this.value = value;
		}

		public Number getValue(Class<? extends Number> _class) { 
			if (Byte.class.equals(_class)) {
				return new Byte(value);
			} else if (Short.class.equals(_class)) {
				return new Short(value);
			} else if (Integer.class.equals(_class)) {
				return new Integer(value);
			} else if (Long.class.equals(_class)) {
				return new Long(value);
			} else if (Float.class.equals(_class)) {
				return new Float(value);
			} else if (Double.class.equals(_class)) {
				return new Double(value);
			}
			throw new IllegalArgumentException("unsupported input class: " + _class.getName());
		}
	}

	private class TestConverter<T extends Number>  implements Converter<T, TestEnum> {
		public TestEnum convert(T source) {
			for (TestEnum testEnum : EnumSet.allOf(TestEnum.class)) {
				if (testEnum.getValue(source.getClass()).equals(source)) {
					return testEnum;
				}
			}
			return null;
		}
	};

	@Test
	public void byteToObjectTest() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new TestConverter<Byte>());

		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.ONE, converter.convertIfNecessary("1", TestEnum.class));
	}

	@Test
	public void shortToObjectTest() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new TestConverter<Short>());

		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.ONE, converter.convertIfNecessary("1", TestEnum.class));
	}

	@Test
	public void integerToObjectTest() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new TestConverter<Integer>());

		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.ONE, converter.convertIfNecessary("1", TestEnum.class));
	}


	@Test
	public void longToObjectTest() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new TestConverter<Long>());

		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.ONE, converter.convertIfNecessary("1", TestEnum.class));
	}

	@Test
	public void floatToObjectTest() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new TestConverter<Float>());

		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.ONE, converter.convertIfNecessary("1", TestEnum.class));
	}

	@Test
	public void doubleToObjectTest() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new TestConverter<Double>());

		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.ONE, converter.convertIfNecessary("1", TestEnum.class));
	}

	@Test(expected = ConversionNotSupportedException.class)
	public void noConverterForString() {
		GenericConversionService service = new GenericConversionService();
		SimpleTypeConverter converter = new SimpleTypeConverter();

		converter.setConversionService(service);

		converter.convertIfNecessary("1", TestEnum.class);
	}

	@Test(expected = ConversionNotSupportedException.class)
	public void noConverterForNumber() {
		GenericConversionService service = new GenericConversionService();
		SimpleTypeConverter converter = new SimpleTypeConverter();

		converter.setConversionService(service);

		converter.convertIfNecessary(1, TestEnum.class);
	}

	/* Test for multiple converters.  Use a byte + long converters, and use a value can 
	 * be parsed as a long but not a byte.
	 */
	@Test
	public void integerToObjectTestMultipleConverters() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new TestConverter<Byte>());
		service.addConverter(new TestConverter<Integer>());

		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.MAX_INT, converter.convertIfNecessary(String.valueOf(Integer.MAX_VALUE), TestEnum.class));
	}

}