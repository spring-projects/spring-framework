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
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import org.junit.Test;
import org.springframework.core.convert.ConversionFailedException;
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
	public void testStringToCharacterEmptyString() {
		StringToCharacterConverter c = new StringToCharacterConverter();
		assertEquals(null, c.convert(""));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testStringToCharacterInvalidString() {
		new StringToCharacterConverter().convert("invalid");
	}

	@Test
	public void testStringToBooleanTrue() {
		StringToBooleanConverter c = new StringToBooleanConverter();
		assertEquals(Boolean.valueOf(true), c.convert("true"));
		assertEquals(Boolean.valueOf(true), c.convert("on"));
		assertEquals(Boolean.valueOf(true), c.convert("yes"));
		assertEquals(Boolean.valueOf(true), c.convert("1"));
	}

	@Test
	public void testStringToBooleanFalse() {
		StringToBooleanConverter c = new StringToBooleanConverter();
		assertEquals(Boolean.valueOf(false), c.convert("false"));
		assertEquals(Boolean.valueOf(false), c.convert("off"));
		assertEquals(Boolean.valueOf(false), c.convert("no"));
		assertEquals(Boolean.valueOf(false), c.convert("0"));
	}

	@Test
	public void testStringToBooleanEmptyString() {
		StringToBooleanConverter c = new StringToBooleanConverter();
		assertEquals(null, c.convert(""));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testStringToBooleanInvalidString() {
		new StringToBooleanConverter().convert("invalid");
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
	public void testStringToNumber() {
		assertEquals(new BigDecimal("1.0"), c.getConverter(Number.class).convert("1.0"));
	}

	@Test
	public void testStringToNumberEmptyString() {
		assertEquals(null, c.getConverter(Number.class).convert(""));
	}

	@Test
	public void testStringToEnum() throws Exception {
		Converter<String, Foo> c = new StringToEnumConverterFactory().getConverter(Foo.class);
		assertEquals(Foo.BAR, c.convert("BAR"));
	}
	
	@Test
	public void testStringToEnumEmptyString() throws Exception {
		Converter<String, Foo> c = new StringToEnumConverterFactory().getConverter(Foo.class);
		assertEquals(null, c.convert(""));
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
	
	@Test
	public void convertObjectToObjectValueOFMethod() {
		DefaultConversionService conversionService = new DefaultConversionService();
		assertEquals(new Integer(3), conversionService.convert("3", Integer.class));
	}

	@Test
	public void convertObjectToObjectConstructor() {
		DefaultConversionService conversionService = new DefaultConversionService();
		assertEquals(new SSN("123456789"), conversionService.convert("123456789", SSN.class));
		assertEquals("123456789", conversionService.convert(new SSN("123456789"), String.class));
	}

	@Test(expected=ConversionFailedException.class)
	public void convertObjectToObjectNoValueOFMethodOrConstructor() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.convert(new Long(3), SSN.class);
	}

	private static class SSN {
		private String value;
		
		public SSN(String value) {
			this.value = value;
		}
		
		public boolean equals(Object o) {
			if (!(o instanceof SSN)) {
				return false;
			}
			SSN ssn = (SSN) o;
			return this.value.equals(ssn.value);
		}
		
		public int hashCode() {
			return value.hashCode();
		}
		
		public String toString() {
			return value;
		}
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
