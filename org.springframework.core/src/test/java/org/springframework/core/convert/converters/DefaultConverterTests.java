package org.springframework.core.convert.converters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import org.junit.Test;
import org.springframework.core.convert.converter.NumberToCharacter;
import org.springframework.core.convert.converter.NumberToNumber;
import org.springframework.core.convert.converter.ObjectToString;
import org.springframework.core.convert.converter.StringToBigDecimal;
import org.springframework.core.convert.converter.StringToBigInteger;
import org.springframework.core.convert.converter.StringToBoolean;
import org.springframework.core.convert.converter.StringToByte;
import org.springframework.core.convert.converter.StringToCharacter;
import org.springframework.core.convert.converter.StringToDouble;
import org.springframework.core.convert.converter.StringToEnum;
import org.springframework.core.convert.converter.StringToFloat;
import org.springframework.core.convert.converter.StringToInteger;
import org.springframework.core.convert.converter.StringToLocale;
import org.springframework.core.convert.converter.StringToLong;
import org.springframework.core.convert.converter.StringToShort;

/**
 * Tests for the default converters in the converters package.
s */
public class DefaultConverterTests {

	@Test
	public void testStringToByte() throws Exception {
		StringToByte b = new StringToByte();
		assertEquals(Byte.valueOf("1"), b.convert("1"));
		assertEquals("1", b.convertBack(Byte.valueOf("1")));
	}

	@Test
	public void testStringToCharacter() {
		StringToCharacter c = new StringToCharacter();
		assertEquals(Character.valueOf('1'), c.convert("1"));
		assertEquals("1", c.convertBack(Character.valueOf('1')));
	}

	@Test
	public void testStringToBoolean() {
		StringToBoolean c = new StringToBoolean();
		assertEquals(Boolean.valueOf(true), c.convert("true"));
		assertEquals(Boolean.valueOf(false), c.convert("false"));
		assertEquals("true", c.convertBack(Boolean.TRUE));
		assertEquals("false", c.convertBack(Boolean.FALSE));
	}
	
	@Test
	public void testStringToBooleanCustomString() {
		StringToBoolean c = new StringToBoolean("yes", "no");
		assertEquals(Boolean.valueOf(true), c.convert("yes"));
		assertEquals(Boolean.valueOf(false), c.convert("no"));
		assertEquals("yes", c.convertBack(Boolean.TRUE));		
		assertEquals("no", c.convertBack(Boolean.FALSE));		
	}
	
	@Test
	public void testStringToBooleanInvalidValue() {
		StringToBoolean c = new StringToBoolean("yes", "no");
		try {
			c.convert("true");
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
			
		}
	}

	@Test
	public void testStringToShort() {
		StringToShort c = new StringToShort();
		assertEquals(Short.valueOf("1"), c.convert("1"));
		assertEquals("1", c.convertBack(Short.valueOf("1")));
	}

	@Test
	public void testStringToInteger() {
		StringToInteger c = new StringToInteger();
		assertEquals(Integer.valueOf("1"), c.convert("1"));
		assertEquals("1", c.convertBack(Integer.valueOf("1")));
	}

	@Test
	public void testStringToLong() {
		StringToLong c = new StringToLong();
		assertEquals(Long.valueOf("1"), c.convert("1"));
		assertEquals("1", c.convertBack(Long.valueOf("1")));	
	}

	@Test
	public void testStringToFloat() {
		StringToFloat c = new StringToFloat();
		assertEquals(Float.valueOf("1.0"), c.convert("1.0"));
		assertEquals("1.0", c.convertBack(Float.valueOf("1.0")));		
	}

	@Test
	public void testStringToDouble() {
		StringToDouble c = new StringToDouble();
		assertEquals(Double.valueOf("1.0"), c.convert("1.0"));
		assertEquals("1.0", c.convertBack(Double.valueOf("1.0")));		
	}

	@Test
	public void testStringToBigInteger() {
		StringToBigInteger c = new StringToBigInteger();
		assertEquals(new BigInteger("1"), c.convert("1"));
		assertEquals("1", c.convertBack(new BigInteger("1")));		
	}

	@Test
	public void testStringToBigDouble() {
		StringToBigDecimal c = new StringToBigDecimal();
		assertEquals(new BigDecimal("1.0"), c.convert("1.0"));
		assertEquals("1.0", c.convertBack(new BigDecimal("1.0")));				
	}

	@Test
	public void testStringToEnum() {
		StringToEnum c = new StringToEnum();
		assertEquals(Foo.BAR, c.convert("BAR", Foo.class));
		assertEquals("BAR", c.convertBack(Foo.BAR, String.class));
	}
	
	public static enum Foo {
		BAR, BAZ;
	}

	@Test
	public void testStringToLocale() {
		StringToLocale c = new StringToLocale();
		assertEquals(Locale.ENGLISH, c.convert("en"));
		assertEquals("en", c.convertBack(Locale.ENGLISH));
	}

	@Test
	public void testNumberToNumber() {
		NumberToNumber n = new NumberToNumber();
		assertEquals(Long.valueOf(1), n.convert(Integer.valueOf(1), Long.class));
	}
	
	@Test
	public void testNumberToNumberNotSupportedNumber() {
		NumberToNumber n = new NumberToNumber();
		try {
			n.convert(Integer.valueOf(1), CustomNumber.class);
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
			
		}
	}
	
	@Test
	public void testNumberToCharacter() {
		NumberToCharacter n = new NumberToCharacter();
		assertEquals(Character.valueOf('A'), n.convert(Integer.valueOf(65), Character.class));
		assertEquals(Integer.valueOf(65), n.convertBack(Character.valueOf('A'), Integer.class));
	}
	
	@Test
	public void testObjectToString() {
		ObjectToString o = new ObjectToString();
		assertEquals("3", o.convert(3, String.class));
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
