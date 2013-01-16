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

import static org.junit.Assert.assertEquals;

import java.util.EnumSet;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

public class NumberToObjectConversionTests {

	private enum TestCase {
		BYTE(
			new TestConverter<?>[] { new TestConverter<Byte>() }
			, "1"
			, TestEnum.ONE
		), SHORT(
			new TestConverter<?>[] { new TestConverter<Short>() }
			, "1"
			, TestEnum.ONE
		), INTEGER(
			new TestConverter<?>[] { new TestConverter<Integer>() }
			, "1"
			, TestEnum.ONE
		), LONG(
			new TestConverter<?>[] { new TestConverter<Long>() }
			, "1"
			, TestEnum.ONE
		), FLOAT(
			new TestConverter<?>[] { new TestConverter<Float>() }
			, "1"
			, TestEnum.ONE
		), DOUBLE(
			new TestConverter<?>[] { new TestConverter<Double>() }
			, "1"
			, TestEnum.ONE
		), BYTE_FAIL_INTEGER_SUCCESS(
			/* Test for multiple converters.  Use a byte + long converters, and use a value can 
			 * be parsed as a long but not a byte.
			 */
			new TestConverter<?>[] { new TestConverter<Byte>(), new TestConverter<Integer>() }
			, String.valueOf(Integer.MAX_VALUE)
			, TestEnum.MAX_INT
		)
		;

		private TestConverter<?>[] converters;
		private String input;
		private TestEnum expectedOutput;

		private TestCase(TestConverter<?>[] converters, String input, TestEnum expectedOutput) {
			this.converters = converters;
			this.input = input;
			this.expectedOutput = expectedOutput;
		}

		public TestConverter<?>[] getConverters() {
			return converters;
		}

		public String getInput() {
			return input;
		}

		public TestEnum getExpectedOutput() {
			return expectedOutput;
		}
	}

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

	private static class TestConverter<T extends Number> implements Converter<T, TestEnum> {
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
	public void validConversionTest() {
		for (TestCase _case : EnumSet.allOf(TestCase.class)) {
			GenericConversionService service = new GenericConversionService();
			for (TestConverter<?> converter : _case.getConverters()) {
				service.addConverter(converter);
			}

			SimpleTypeConverter converter = new SimpleTypeConverter();
			converter.setConversionService(service);

			assertEquals(_case.getExpectedOutput(), converter.convertIfNecessary(_case.getInput(), TestEnum.class));
		}
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
}