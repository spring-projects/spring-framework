/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class NumberUtilsTests {

	@Test
	public void parseNumber() {
		String aByte = "" + Byte.MAX_VALUE;
		String aShort = "" + Short.MAX_VALUE;
		String anInteger = "" + Integer.MAX_VALUE;
		String aLong = "" + Long.MAX_VALUE;
		String aFloat = "" + Float.MAX_VALUE;
		String aDouble = "" + Double.MAX_VALUE;

		assertEquals("Byte did not parse", new Byte(Byte.MAX_VALUE), NumberUtils.parseNumber(aByte, Byte.class));
		assertEquals("Short did not parse", new Short(Short.MAX_VALUE), NumberUtils.parseNumber(aShort, Short.class));
		assertEquals("Integer did not parse", new Integer(Integer.MAX_VALUE), NumberUtils.parseNumber(anInteger, Integer.class));
		assertEquals("Long did not parse", new Long(Long.MAX_VALUE), NumberUtils.parseNumber(aLong, Long.class));
		assertEquals("Float did not parse", new Float(Float.MAX_VALUE), NumberUtils.parseNumber(aFloat, Float.class));
		assertEquals("Double did not parse", new Double(Double.MAX_VALUE), NumberUtils.parseNumber(aDouble, Double.class));
	}

	@Test
	public void parseNumberUsingNumberFormat() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		String aByte = "" + Byte.MAX_VALUE;
		String aShort = "" + Short.MAX_VALUE;
		String anInteger = "" + Integer.MAX_VALUE;
		String aLong = "" + Long.MAX_VALUE;
		String aFloat = "" + Float.MAX_VALUE;
		String aDouble = "" + Double.MAX_VALUE;

		assertEquals("Byte did not parse", new Byte(Byte.MAX_VALUE), NumberUtils.parseNumber(aByte, Byte.class, nf));
		assertEquals("Short did not parse", new Short(Short.MAX_VALUE), NumberUtils.parseNumber(aShort, Short.class, nf));
		assertEquals("Integer did not parse", new Integer(Integer.MAX_VALUE), NumberUtils.parseNumber(anInteger, Integer.class, nf));
		assertEquals("Long did not parse", new Long(Long.MAX_VALUE), NumberUtils.parseNumber(aLong, Long.class, nf));
		assertEquals("Float did not parse", new Float(Float.MAX_VALUE), NumberUtils.parseNumber(aFloat, Float.class, nf));
		assertEquals("Double did not parse", new Double(Double.MAX_VALUE), NumberUtils.parseNumber(aDouble, Double.class, nf));
	}

	@Test
	public void parseNumberRequiringTrim() {
		String aByte = " " + Byte.MAX_VALUE + " ";
		String aShort = " " + Short.MAX_VALUE + " ";
		String anInteger = " " + Integer.MAX_VALUE + " ";
		String aLong = " " + Long.MAX_VALUE + " ";
		String aFloat = " " + Float.MAX_VALUE + " ";
		String aDouble = " " + Double.MAX_VALUE + " ";

		assertEquals("Byte did not parse", new Byte(Byte.MAX_VALUE), NumberUtils.parseNumber(aByte, Byte.class));
		assertEquals("Short did not parse", new Short(Short.MAX_VALUE), NumberUtils.parseNumber(aShort, Short.class));
		assertEquals("Integer did not parse", new Integer(Integer.MAX_VALUE), NumberUtils.parseNumber(anInteger, Integer.class));
		assertEquals("Long did not parse", new Long(Long.MAX_VALUE), NumberUtils.parseNumber(aLong, Long.class));
		assertEquals("Float did not parse", new Float(Float.MAX_VALUE), NumberUtils.parseNumber(aFloat, Float.class));
		assertEquals("Double did not parse", new Double(Double.MAX_VALUE), NumberUtils.parseNumber(aDouble, Double.class));
	}

	@Test
	public void parseNumberRequiringTrimUsingNumberFormat() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		String aByte = " " + Byte.MAX_VALUE + " ";
		String aShort = " " + Short.MAX_VALUE + " ";
		String anInteger = " " + Integer.MAX_VALUE + " ";
		String aLong = " " + Long.MAX_VALUE + " ";
		String aFloat = " " + Float.MAX_VALUE + " ";
		String aDouble = " " + Double.MAX_VALUE + " ";

		assertEquals("Byte did not parse", new Byte(Byte.MAX_VALUE), NumberUtils.parseNumber(aByte, Byte.class, nf));
		assertEquals("Short did not parse", new Short(Short.MAX_VALUE), NumberUtils.parseNumber(aShort, Short.class, nf));
		assertEquals("Integer did not parse", new Integer(Integer.MAX_VALUE), NumberUtils.parseNumber(anInteger, Integer.class, nf));
		assertEquals("Long did not parse", new Long(Long.MAX_VALUE), NumberUtils.parseNumber(aLong, Long.class, nf));
		assertEquals("Float did not parse", new Float(Float.MAX_VALUE), NumberUtils.parseNumber(aFloat, Float.class, nf));
		assertEquals("Double did not parse", new Double(Double.MAX_VALUE), NumberUtils.parseNumber(aDouble, Double.class, nf));
	}

	@Test
	public void parseNumberAsHex() {
		String aByte = "0x" + Integer.toHexString(new Byte(Byte.MAX_VALUE).intValue());
		String aShort = "0x" + Integer.toHexString(new Short(Short.MAX_VALUE).intValue());
		String anInteger = "0x" + Integer.toHexString(Integer.MAX_VALUE);
		String aLong = "0x" + Long.toHexString(Long.MAX_VALUE);
		String aReallyBigInt = "FEBD4E677898DFEBFFEE44";

		assertByteEquals(aByte);
		assertShortEquals(aShort);
		assertIntegerEquals(anInteger);
		assertLongEquals(aLong);
		assertEquals("BigInteger did not parse",
				new BigInteger(aReallyBigInt, 16), NumberUtils.parseNumber("0x" + aReallyBigInt, BigInteger.class));
	}

	@Test
	public void parseNumberAsNegativeHex() {
		String aByte = "-0x80";
		String aShort = "-0x8000";
		String anInteger = "-0x80000000";
		String aLong = "-0x8000000000000000";
		String aReallyBigInt = "FEBD4E677898DFEBFFEE44";

		assertNegativeByteEquals(aByte);
		assertNegativeShortEquals(aShort);
		assertNegativeIntegerEquals(anInteger);
		assertNegativeLongEquals(aLong);
		assertEquals("BigInteger did not parse",
				new BigInteger(aReallyBigInt, 16).negate(), NumberUtils.parseNumber("-0x" + aReallyBigInt, BigInteger.class));
	}

	@Test
	public void convertDoubleToBigInteger() {
		Double decimal = new Double(3.14d);
		assertEquals(new BigInteger("3"), NumberUtils.convertNumberToTargetClass(decimal, BigInteger.class));
	}

	@Test
	public void convertBigDecimalToBigInteger() {
		String number = "987459837583750387355346";
		BigDecimal decimal = new BigDecimal(number);
		assertEquals(new BigInteger(number), NumberUtils.convertNumberToTargetClass(decimal, BigInteger.class));
	}

	@Test
	public void convertNonExactBigDecimalToBigInteger() {
		BigDecimal decimal = new BigDecimal("987459837583750387355346.14");
		assertEquals(new BigInteger("987459837583750387355346"), NumberUtils.convertNumberToTargetClass(decimal, BigInteger.class));
	}

	@Test
	public void parseBigDecimalNumber1() {
		String bigDecimalAsString = "0.10";
		Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class);
		assertEquals(new BigDecimal(bigDecimalAsString), bigDecimal);
	}

	@Test
	public void parseBigDecimalNumber2() {
		String bigDecimalAsString = "0.001";
		Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class);
		assertEquals(new BigDecimal(bigDecimalAsString), bigDecimal);
	}

	@Test
	public void parseBigDecimalNumber3() {
		String bigDecimalAsString = "3.14159265358979323846";
		Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class);
		assertEquals(new BigDecimal(bigDecimalAsString), bigDecimal);
	}

	@Test
	public void parseLocalizedBigDecimalNumber1() {
		String bigDecimalAsString = "0.10";
		NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
		Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class, numberFormat);
		assertEquals(new BigDecimal(bigDecimalAsString), bigDecimal);
	}

	@Test
	public void parseLocalizedBigDecimalNumber2() {
		String bigDecimalAsString = "0.001";
		NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
		Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class, numberFormat);
		assertEquals(new BigDecimal(bigDecimalAsString), bigDecimal);
	}

	@Test
	public void parseLocalizedBigDecimalNumber3() {
		String bigDecimalAsString = "3.14159265358979323846";
		NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
		Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class, numberFormat);
		assertEquals(new BigDecimal(bigDecimalAsString), bigDecimal);
	}

	@Test
	public void parseOverflow() {
		String aLong = "" + Long.MAX_VALUE;
		String aDouble = "" + Double.MAX_VALUE;

		try {
			NumberUtils.parseNumber(aLong, Byte.class);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}

		try {
			NumberUtils.parseNumber(aLong, Short.class);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}

		try {
			NumberUtils.parseNumber(aLong, Integer.class);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}

		assertEquals(new Long(Long.MAX_VALUE), NumberUtils.parseNumber(aLong, Long.class));
		assertEquals(new Double(Double.MAX_VALUE), NumberUtils.parseNumber(aDouble, Double.class));
	}

	@Test
	public void parseNegativeOverflow() {
		String aLong = "" + Long.MIN_VALUE;
		String aDouble = "" + Double.MIN_VALUE;

		try {
			NumberUtils.parseNumber(aLong, Byte.class);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}

		try {
			NumberUtils.parseNumber(aLong, Short.class);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}

		try {
			NumberUtils.parseNumber(aLong, Integer.class);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}

		assertEquals(new Long(Long.MIN_VALUE), NumberUtils.parseNumber(aLong, Long.class));
		assertEquals(new Double(Double.MIN_VALUE), NumberUtils.parseNumber(aDouble, Double.class));
	}

	@Test
	public void parseOverflowUsingNumberFormat() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		String aLong = "" + Long.MAX_VALUE;
		String aDouble = "" + Double.MAX_VALUE;

		try {
			NumberUtils.parseNumber(aLong, Byte.class, nf);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}

		try {
			NumberUtils.parseNumber(aLong, Short.class, nf);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}

		try {
			NumberUtils.parseNumber(aLong, Integer.class, nf);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}

		assertEquals(new Long(Long.MAX_VALUE), NumberUtils.parseNumber(aLong, Long.class, nf));
		assertEquals(new Double(Double.MAX_VALUE), NumberUtils.parseNumber(aDouble, Double.class, nf));
	}

	@Test
	public void parseNegativeOverflowUsingNumberFormat() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		String aLong = "" + Long.MIN_VALUE;
		String aDouble = "" + Double.MIN_VALUE;

		try {
			NumberUtils.parseNumber(aLong, Byte.class, nf);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}

		try {
			NumberUtils.parseNumber(aLong, Short.class, nf);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}

		try {
			NumberUtils.parseNumber(aLong, Integer.class, nf);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}

		assertEquals(new Long(Long.MIN_VALUE), NumberUtils.parseNumber(aLong, Long.class, nf));
		assertEquals(new Double(Double.MIN_VALUE), NumberUtils.parseNumber(aDouble, Double.class, nf));
	}

	@Test
	public void convertToInteger() {
		assertEquals(Integer.valueOf(Integer.valueOf(-1)), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(-1), Integer.class));
		assertEquals(Integer.valueOf(Integer.valueOf(0)), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(0), Integer.class));
		assertEquals(Integer.valueOf(Integer.valueOf(1)), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(1), Integer.class));
		assertEquals(Integer.valueOf(Integer.MAX_VALUE), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Integer.MAX_VALUE), Integer.class));
		assertEquals(Integer.valueOf(Integer.MIN_VALUE), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Integer.MAX_VALUE + 1), Integer.class));
		assertEquals(Integer.valueOf(Integer.MIN_VALUE), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Integer.MIN_VALUE), Integer.class));
		assertEquals(Integer.valueOf(Integer.MAX_VALUE), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Integer.MIN_VALUE - 1), Integer.class));
		assertToNumberOverflow(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE), Integer.class);
		assertToNumberOverflow(BigInteger.valueOf(Integer.MIN_VALUE).subtract(BigInteger.ONE), Integer.class);

		assertEquals(Integer.valueOf(Integer.valueOf(-1)), NumberUtils.convertNumberToTargetClass(Long.valueOf(-1), Integer.class));
		assertEquals(Integer.valueOf(Integer.valueOf(0)), NumberUtils.convertNumberToTargetClass(Long.valueOf(0), Integer.class));
		assertEquals(Integer.valueOf(Integer.valueOf(1)), NumberUtils.convertNumberToTargetClass(Long.valueOf(1), Integer.class));
		assertEquals(Integer.valueOf(Integer.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Long.valueOf(Integer.MAX_VALUE), Integer.class));
		assertEquals(Integer.valueOf(Integer.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Long.valueOf(Integer.MAX_VALUE + 1), Integer.class));
		assertEquals(Integer.valueOf(Integer.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Long.valueOf(Integer.MIN_VALUE), Integer.class));
		assertEquals(Integer.valueOf(Integer.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Long.valueOf(Integer.MIN_VALUE - 1), Integer.class));
		assertToNumberOverflow(Long.valueOf(Long.MAX_VALUE + 1), Integer.class);
		assertToNumberOverflow(Long.valueOf(Long.MIN_VALUE - 1), Integer.class);

		assertEquals(Integer.valueOf(Integer.valueOf(-1)), NumberUtils.convertNumberToTargetClass(Integer.valueOf(-1), Integer.class));
		assertEquals(Integer.valueOf(Integer.valueOf(0)), NumberUtils.convertNumberToTargetClass(Integer.valueOf(0), Integer.class));
		assertEquals(Integer.valueOf(Integer.valueOf(1)), NumberUtils.convertNumberToTargetClass(Integer.valueOf(1), Integer.class));
		assertEquals(Integer.valueOf(Integer.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MAX_VALUE), Integer.class));
		assertEquals(Integer.valueOf(Integer.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MAX_VALUE + 1), Integer.class));
		assertEquals(Integer.valueOf(Integer.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MIN_VALUE), Integer.class));
		assertEquals(Integer.valueOf(Integer.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MIN_VALUE - 1), Integer.class));

		assertEquals(Integer.valueOf(Integer.valueOf(-1)), NumberUtils.convertNumberToTargetClass(Short.valueOf((short) -1), Integer.class));
		assertEquals(Integer.valueOf(Integer.valueOf(0)), NumberUtils.convertNumberToTargetClass(Short.valueOf((short) 0), Integer.class));
		assertEquals(Integer.valueOf(Integer.valueOf(1)), NumberUtils.convertNumberToTargetClass(Short.valueOf((short) 1), Integer.class));
		assertEquals(Integer.valueOf(Short.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Short.valueOf(Short.MAX_VALUE), Integer.class));
		assertEquals(Integer.valueOf(Short.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Short.valueOf((short) (Short.MAX_VALUE + 1)), Integer.class));
		assertEquals(Integer.valueOf(Short.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Short.valueOf(Short.MIN_VALUE), Integer.class));
		assertEquals(Integer.valueOf(Short.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Short.valueOf((short) (Short.MIN_VALUE - 1)), Integer.class));

		assertEquals(Integer.valueOf(Integer.valueOf(-1)), NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) -1), Integer.class));
		assertEquals(Integer.valueOf(Integer.valueOf(0)), NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) 0), Integer.class));
		assertEquals(Integer.valueOf(Integer.valueOf(1)), NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) 1), Integer.class));
		assertEquals(Integer.valueOf(Byte.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Byte.valueOf(Byte.MAX_VALUE), Integer.class));
		assertEquals(Integer.valueOf(Byte.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) (Byte.MAX_VALUE + 1)), Integer.class));
		assertEquals(Integer.valueOf(Byte.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Byte.valueOf(Byte.MIN_VALUE), Integer.class));
		assertEquals(Integer.valueOf(Byte.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) (Byte.MIN_VALUE - 1)), Integer.class));
	}

	@Test
	public void convertToLong() {
		assertEquals(Long.valueOf(Long.valueOf(-1)), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(-1), Long.class));
		assertEquals(Long.valueOf(Long.valueOf(0)), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(0), Long.class));
		assertEquals(Long.valueOf(Long.valueOf(1)), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(1), Long.class));
		assertEquals(Long.valueOf(Long.MAX_VALUE), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Long.MAX_VALUE), Long.class));
		assertEquals(Long.valueOf(Long.MIN_VALUE), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Long.MAX_VALUE + 1), Long.class));
		assertEquals(Long.valueOf(Long.MIN_VALUE), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Long.MIN_VALUE), Long.class));
		assertEquals(Long.valueOf(Long.MAX_VALUE), NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Long.MIN_VALUE - 1), Long.class));

		assertToNumberOverflow(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), Long.class);
		assertToNumberOverflow(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE), Long.class);

		assertEquals(Long.valueOf(Long.valueOf(-1)), NumberUtils.convertNumberToTargetClass(Long.valueOf(-1), Long.class));
		assertEquals(Long.valueOf(Long.valueOf(0)), NumberUtils.convertNumberToTargetClass(Long.valueOf(0), Long.class));
		assertEquals(Long.valueOf(Long.valueOf(1)), NumberUtils.convertNumberToTargetClass(Long.valueOf(1), Long.class));
		assertEquals(Long.valueOf(Long.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Long.valueOf(Long.MAX_VALUE), Long.class));
		assertEquals(Long.valueOf(Long.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Long.valueOf(Long.MAX_VALUE + 1), Long.class));
		assertEquals(Long.valueOf(Long.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Long.valueOf(Long.MIN_VALUE), Long.class));
		assertEquals(Long.valueOf(Long.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Long.valueOf(Long.MIN_VALUE - 1), Long.class));

		assertEquals(Long.valueOf(Integer.valueOf(-1)), NumberUtils.convertNumberToTargetClass(Integer.valueOf(-1), Long.class));
		assertEquals(Long.valueOf(Integer.valueOf(0)), NumberUtils.convertNumberToTargetClass(Integer.valueOf(0), Long.class));
		assertEquals(Long.valueOf(Integer.valueOf(1)), NumberUtils.convertNumberToTargetClass(Integer.valueOf(1), Long.class));
		assertEquals(Long.valueOf(Integer.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MAX_VALUE), Long.class));
		assertEquals(Long.valueOf(Integer.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MAX_VALUE + 1), Long.class));
		assertEquals(Long.valueOf(Integer.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MIN_VALUE), Long.class));
		assertEquals(Long.valueOf(Integer.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MIN_VALUE - 1), Long.class));

		assertEquals(Long.valueOf(Integer.valueOf(-1)), NumberUtils.convertNumberToTargetClass(Short.valueOf((short) -1), Long.class));
		assertEquals(Long.valueOf(Integer.valueOf(0)), NumberUtils.convertNumberToTargetClass(Short.valueOf((short) 0), Long.class));
		assertEquals(Long.valueOf(Integer.valueOf(1)), NumberUtils.convertNumberToTargetClass(Short.valueOf((short) 1), Long.class));
		assertEquals(Long.valueOf(Short.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Short.valueOf(Short.MAX_VALUE), Long.class));
		assertEquals(Long.valueOf(Short.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Short.valueOf((short) (Short.MAX_VALUE + 1)), Long.class));
		assertEquals(Long.valueOf(Short.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Short.valueOf(Short.MIN_VALUE), Long.class));
		assertEquals(Long.valueOf(Short.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Short.valueOf((short) (Short.MIN_VALUE - 1)), Long.class));

		assertEquals(Long.valueOf(Integer.valueOf(-1)), NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) -1), Long.class));
		assertEquals(Long.valueOf(Integer.valueOf(0)), NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) 0), Long.class));
		assertEquals(Long.valueOf(Integer.valueOf(1)), NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) 1), Long.class));
		assertEquals(Long.valueOf(Byte.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Byte.valueOf(Byte.MAX_VALUE), Long.class));
		assertEquals(Long.valueOf(Byte.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) (Byte.MAX_VALUE + 1)), Long.class));
		assertEquals(Long.valueOf(Byte.MIN_VALUE), NumberUtils.convertNumberToTargetClass(Byte.valueOf(Byte.MIN_VALUE), Long.class));
		assertEquals(Long.valueOf(Byte.MAX_VALUE), NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) (Byte.MIN_VALUE - 1)), Long.class));
	}


	private void assertLongEquals(String aLong) {
		assertEquals("Long did not parse", Long.MAX_VALUE, NumberUtils.parseNumber(aLong, Long.class).longValue());
	}

	private void assertIntegerEquals(String anInteger) {
		assertEquals("Integer did not parse", Integer.MAX_VALUE, NumberUtils.parseNumber(anInteger, Integer.class).intValue());
	}

	private void assertShortEquals(String aShort) {
		assertEquals("Short did not parse", Short.MAX_VALUE, NumberUtils.parseNumber(aShort, Short.class).shortValue());
	}

	private void assertByteEquals(String aByte) {
		assertEquals("Byte did not parse", Byte.MAX_VALUE, NumberUtils.parseNumber(aByte, Byte.class).byteValue());
	}

	private void assertNegativeLongEquals(String aLong) {
		assertEquals("Long did not parse", Long.MIN_VALUE, NumberUtils.parseNumber(aLong, Long.class).longValue());
	}

	private void assertNegativeIntegerEquals(String anInteger) {
		assertEquals("Integer did not parse", Integer.MIN_VALUE, NumberUtils.parseNumber(anInteger, Integer.class).intValue());
	}

	private void assertNegativeShortEquals(String aShort) {
		assertEquals("Short did not parse", Short.MIN_VALUE, NumberUtils.parseNumber(aShort, Short.class).shortValue());
	}

	private void assertNegativeByteEquals(String aByte) {
		assertEquals("Byte did not parse", Byte.MIN_VALUE, NumberUtils.parseNumber(aByte, Byte.class).byteValue());
	}

	private void assertToNumberOverflow(Number number, Class<? extends Number> targetClass) {
		String msg = "Expected exception due to overflow: from=" + number + ", toClass=" + targetClass;
		try {
			NumberUtils.convertNumberToTargetClass(number, targetClass);
			fail(msg);
		}
		catch (IllegalArgumentException expected) {
			assertTrue(msg + ", with \"overflow\" in message but got message=" + expected.getMessage(),
					expected.getMessage().endsWith("overflow"));
		}
	}

}
