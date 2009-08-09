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
import org.springframework.core.convert.support.NumberToCharacter;
import org.springframework.core.convert.support.NumberToNumberFactory;
import org.springframework.core.convert.support.ObjectToString;
import org.springframework.core.convert.support.StringToBigDecimal;
import org.springframework.core.convert.support.StringToBigInteger;
import org.springframework.core.convert.support.StringToBoolean;
import org.springframework.core.convert.support.StringToByte;
import org.springframework.core.convert.support.StringToCharacter;
import org.springframework.core.convert.support.StringToDouble;
import org.springframework.core.convert.support.StringToEnumFactory;
import org.springframework.core.convert.support.StringToFloat;
import org.springframework.core.convert.support.StringToInteger;
import org.springframework.core.convert.support.StringToLocale;
import org.springframework.core.convert.support.StringToLong;
import org.springframework.core.convert.support.StringToShort;

/**
 * Tests for the default converters in the converters package.

 * @author Keith Donald
 */
public class DefaultConversionServiceTests {

	@Test
	public void testStringToByte() throws Exception {
		StringToByte b = new StringToByte();
		assertEquals(Byte.valueOf("1"), b.convert("1"));
	}

	@Test
	public void testStringToCharacter() {
		StringToCharacter c = new StringToCharacter();
		assertEquals(Character.valueOf('1'), c.convert("1"));
	}

	@Test
	public void testStringToBoolean() {
		StringToBoolean c = new StringToBoolean();
		assertEquals(Boolean.valueOf(true), c.convert("true"));
		assertEquals(Boolean.valueOf(false), c.convert("false"));
	}
	
	@Test
	public void testStringToShort() {
		StringToShort c = new StringToShort();
		assertEquals(Short.valueOf("1"), c.convert("1"));
	}

	@Test
	public void testStringToInteger() {
		StringToInteger c = new StringToInteger();
		assertEquals(Integer.valueOf("1"), c.convert("1"));
	}

	@Test
	public void testStringToLong() {
		StringToLong c = new StringToLong();
		assertEquals(Long.valueOf("1"), c.convert("1"));
	}

	@Test
	public void testStringToFloat() {
		StringToFloat c = new StringToFloat();
		assertEquals(Float.valueOf("1.0"), c.convert("1.0"));
	}

	@Test
	public void testStringToDouble() {
		StringToDouble c = new StringToDouble();
		assertEquals(Double.valueOf("1.0"), c.convert("1.0"));
	}

	@Test
	public void testStringToBigInteger() {
		StringToBigInteger c = new StringToBigInteger();
		assertEquals(new BigInteger("1"), c.convert("1"));
	}

	@Test
	public void testStringToBigDouble() {
		StringToBigDecimal c = new StringToBigDecimal();
		assertEquals(new BigDecimal("1.0"), c.convert("1.0"));
	}

	@Test
	public void testStringToEnum() throws Exception {
		Converter<String, Foo> c = new StringToEnumFactory().getConverter(Foo.class);
		assertEquals(Foo.BAR, c.convert("BAR"));
	}
	
	public static enum Foo {
		BAR, BAZ;
	}

	@Test
	public void testStringToLocale() {
		StringToLocale c = new StringToLocale();
		assertEquals(Locale.ENGLISH, c.convert("en"));
	}

	@Test
	public void testNumberToNumber() throws Exception {
		Converter<Number, Long> c = new NumberToNumberFactory().getConverter(Long.class);
		assertEquals(Long.valueOf(1), c.convert(Integer.valueOf(1)));
	}
	
	@Test
	public void testNumberToNumberNotSupportedNumber() throws Exception {
		Converter<Number, CustomNumber> c = new NumberToNumberFactory().getConverter(CustomNumber.class);
		try {
			c.convert(Integer.valueOf(1));
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
			
		}
	}
	
	@Test
	public void testNumberToCharacter() {
		NumberToCharacter n = new NumberToCharacter();
		assertEquals(Character.valueOf('A'), n.convert(Integer.valueOf(65)));
	}
	
	@Test
	public void testObjectToString() {
		ObjectToString o = new ObjectToString();
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
