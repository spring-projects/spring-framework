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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;

/**
 * Tests for the default converters in the converters package.

 * @author Keith Donald
 */
public class DefaultConversionServiceTests {

	private StringToNumberConverterFactory c = new StringToNumberConverterFactory();
	
	@Test
	public void testStringToCharacter() {
		StringToCharacterConverter c = new StringToCharacterConverter();
		assertEquals(Character.valueOf('1'), c.convert("1"));
	}

	@Test
	public void testStringToBoolean() {
		StringToBooleanConverter c = new StringToBooleanConverter();
		assertEquals(Boolean.valueOf(true), c.convert("true"));
		assertEquals(Boolean.valueOf(false), c.convert("false"));
	}

	@Test
	public void testStringToByte() throws Exception {
		assertEquals(Byte.valueOf("1"), c.getConverter(Byte.class).convert("1"));
	}

	@Test
	public void testStringToShort() {
		assertEquals(Short.valueOf("1"), c.getConverter(Short.class).convert("1"));
	}

	@Test
	public void testStringToInteger() {
		assertEquals(Integer.valueOf("1"), c.getConverter(Integer.class).convert("1"));
	}

	@Test
	public void testStringToLong() {
		assertEquals(Long.valueOf("1"), c.getConverter(Long.class).convert("1"));
	}

	@Test
	public void testStringToFloat() {
		assertEquals(Float.valueOf("1.0"), c.getConverter(Float.class).convert("1.0"));
	}

	@Test
	public void testStringToDouble() {
		assertEquals(Double.valueOf("1.0"), c.getConverter(Double.class).convert("1.0"));
	}

	@Test
	public void testStringToBigInteger() {
		assertEquals(new BigInteger("1"), c.getConverter(BigInteger.class).convert("1"));
	}

	@Test
	public void testStringToBigDouble() {
		assertEquals(new BigDecimal("1.0"), c.getConverter(BigDecimal.class).convert("1.0"));
	}

	@Test
	public void testStringToEnum() throws Exception {
		Converter<String, Foo> c = new StringToEnumConverterFactory().getConverter(Foo.class);
		assertEquals(Foo.BAR, c.convert("BAR"));
	}
	
	public static enum Foo {
		BAR, BAZ;
	}

	@Test
	public void testStringToLocale() {
		StringToLocaleConverter c = new StringToLocaleConverter();
		assertEquals(Locale.ENGLISH, c.convert("en"));
	}

	@Test
	public void testNumberToNumber() throws Exception {
		Converter<Number, Long> c = new NumberToNumberConverterFactory().getConverter(Long.class);
		assertEquals(Long.valueOf(1), c.convert(Integer.valueOf(1)));
	}
	
	@Test
	public void testNumberToNumberNotSupportedNumber() throws Exception {
		Converter<Number, CustomNumber> c = new NumberToNumberConverterFactory().getConverter(CustomNumber.class);
		try {
			c.convert(Integer.valueOf(1));
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
			
		}
	}
	
	@Test
	public void testNumberToCharacter() {
		NumberToCharacterConverter n = new NumberToCharacterConverter();
		assertEquals(Character.valueOf('A'), n.convert(Integer.valueOf(65)));
	}
	
	@Test
	public void testObjectToString() {
		ObjectToStringConverter o = new ObjectToStringConverter();
		assertEquals("3", o.convert(3));
	}
	
	public static class CustomNumber extends Number {

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
		
	}
}
