/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class NumberUtilsTests {

	@Test
	void parseNumber() {
		String aByte = "" + Byte.MAX_VALUE;
		String aShort = "" + Short.MAX_VALUE;
		String anInteger = "" + Integer.MAX_VALUE;
		String aLong = "" + Long.MAX_VALUE;
		String aFloat = "" + Float.MAX_VALUE;
		String aDouble = "" + Double.MAX_VALUE;

		assertThat(NumberUtils.parseNumber(aByte, Byte.class)).as("Byte did not parse").isEqualTo(Byte.valueOf(Byte.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aShort, Short.class)).as("Short did not parse").isEqualTo(Short.valueOf(Short.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(anInteger, Integer.class)).as("Integer did not parse").isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aLong, Long.class)).as("Long did not parse").isEqualTo(Long.valueOf(Long.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aFloat, Float.class)).as("Float did not parse").isEqualTo(Float.valueOf(Float.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aDouble, Double.class)).as("Double did not parse").isEqualTo(Double.valueOf(Double.MAX_VALUE));
	}

	@Test
	void parseNumberUsingNumberFormat() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		String aByte = "" + Byte.MAX_VALUE;
		String aShort = "" + Short.MAX_VALUE;
		String anInteger = "" + Integer.MAX_VALUE;
		String aLong = "" + Long.MAX_VALUE;
		String aFloat = "" + Float.MAX_VALUE;
		String aDouble = "" + Double.MAX_VALUE;

		assertThat(NumberUtils.parseNumber(aByte, Byte.class, nf)).as("Byte did not parse").isEqualTo(Byte.valueOf(Byte.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aShort, Short.class, nf)).as("Short did not parse").isEqualTo(Short.valueOf(Short.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(anInteger, Integer.class, nf)).as("Integer did not parse").isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aLong, Long.class, nf)).as("Long did not parse").isEqualTo(Long.valueOf(Long.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aFloat, Float.class, nf)).as("Float did not parse").isEqualTo(Float.valueOf(Float.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aDouble, Double.class, nf)).as("Double did not parse").isEqualTo(Double.valueOf(Double.MAX_VALUE));
	}

	@Test
	void parseNumberRequiringTrim() {
		String aByte = " " + Byte.MAX_VALUE + " ";
		String aShort = " " + Short.MAX_VALUE + " ";
		String anInteger = " " + Integer.MAX_VALUE + " ";
		String aLong = " " + Long.MAX_VALUE + " ";
		String aFloat = " " + Float.MAX_VALUE + " ";
		String aDouble = " " + Double.MAX_VALUE + " ";

		assertThat(NumberUtils.parseNumber(aByte, Byte.class)).as("Byte did not parse").isEqualTo(Byte.valueOf(Byte.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aShort, Short.class)).as("Short did not parse").isEqualTo(Short.valueOf(Short.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(anInteger, Integer.class)).as("Integer did not parse").isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aLong, Long.class)).as("Long did not parse").isEqualTo(Long.valueOf(Long.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aFloat, Float.class)).as("Float did not parse").isEqualTo(Float.valueOf(Float.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aDouble, Double.class)).as("Double did not parse").isEqualTo(Double.valueOf(Double.MAX_VALUE));
	}

	@Test
	void parseNumberRequiringTrimUsingNumberFormat() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		String aByte = " " + Byte.MAX_VALUE + " ";
		String aShort = " " + Short.MAX_VALUE + " ";
		String anInteger = " " + Integer.MAX_VALUE + " ";
		String aLong = " " + Long.MAX_VALUE + " ";
		String aFloat = " " + Float.MAX_VALUE + " ";
		String aDouble = " " + Double.MAX_VALUE + " ";

		assertThat(NumberUtils.parseNumber(aByte, Byte.class, nf)).as("Byte did not parse").isEqualTo(Byte.valueOf(Byte.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aShort, Short.class, nf)).as("Short did not parse").isEqualTo(Short.valueOf(Short.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(anInteger, Integer.class, nf)).as("Integer did not parse").isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aLong, Long.class, nf)).as("Long did not parse").isEqualTo(Long.valueOf(Long.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aFloat, Float.class, nf)).as("Float did not parse").isEqualTo(Float.valueOf(Float.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aDouble, Double.class, nf)).as("Double did not parse").isEqualTo(Double.valueOf(Double.MAX_VALUE));
	}

	@Test
	void parseNumberAsHex() {
		String aByte = "0x" + Integer.toHexString(Byte.valueOf(Byte.MAX_VALUE));
		String aShort = "0x" + Integer.toHexString(Short.valueOf(Short.MAX_VALUE));
		String anInteger = "0x" + Integer.toHexString(Integer.MAX_VALUE);
		String aLong = "0x" + Long.toHexString(Long.MAX_VALUE);
		String aReallyBigInt = "FEBD4E677898DFEBFFEE44";

		assertByteEquals(aByte);
		assertShortEquals(aShort);
		assertIntegerEquals(anInteger);
		assertLongEquals(aLong);
		assertThat(NumberUtils.parseNumber("0x" + aReallyBigInt, BigInteger.class)).as("BigInteger did not parse").isEqualTo(new BigInteger(aReallyBigInt, 16));
	}

	@Test
	void parseNumberAsNegativeHex() {
		String aByte = "-0x80";
		String aShort = "-0x8000";
		String anInteger = "-0x80000000";
		String aLong = "-0x8000000000000000";
		String aReallyBigInt = "FEBD4E677898DFEBFFEE44";

		assertNegativeByteEquals(aByte);
		assertNegativeShortEquals(aShort);
		assertNegativeIntegerEquals(anInteger);
		assertNegativeLongEquals(aLong);
		assertThat(NumberUtils.parseNumber("-0x" + aReallyBigInt, BigInteger.class)).as("BigInteger did not parse").isEqualTo(new BigInteger(aReallyBigInt, 16).negate());
	}

	@Test
	void convertDoubleToBigInteger() {
		Double decimal = 3.14d;
		assertThat(NumberUtils.convertNumberToTargetClass(decimal, BigInteger.class)).isEqualTo(new BigInteger("3"));
	}

	@Test
	void convertBigDecimalToBigInteger() {
		String number = "987459837583750387355346";
		BigDecimal decimal = new BigDecimal(number);
		assertThat(NumberUtils.convertNumberToTargetClass(decimal, BigInteger.class)).isEqualTo(new BigInteger(number));
	}

	@Test
	void convertNonExactBigDecimalToBigInteger() {
		BigDecimal decimal = new BigDecimal("987459837583750387355346.14");
		assertThat(NumberUtils.convertNumberToTargetClass(decimal, BigInteger.class)).isEqualTo(new BigInteger("987459837583750387355346"));
	}

	@Test
	void parseBigDecimalNumber1() {
		String bigDecimalAsString = "0.10";
		Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class);
		assertThat(bigDecimal).isEqualTo(new BigDecimal(bigDecimalAsString));
	}

	@Test
	void parseBigDecimalNumber2() {
		String bigDecimalAsString = "0.001";
		Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class);
		assertThat(bigDecimal).isEqualTo(new BigDecimal(bigDecimalAsString));
	}

	@Test
	void parseBigDecimalNumber3() {
		String bigDecimalAsString = "3.14159265358979323846";
		Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class);
		assertThat(bigDecimal).isEqualTo(new BigDecimal(bigDecimalAsString));
	}

	@Test
	void parseLocalizedBigDecimalNumber1() {
		String bigDecimalAsString = "0.10";
		NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
		Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class, numberFormat);
		assertThat(bigDecimal).isEqualTo(new BigDecimal(bigDecimalAsString));
	}

	@Test
	void parseLocalizedBigDecimalNumber2() {
		String bigDecimalAsString = "0.001";
		NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
		Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class, numberFormat);
		assertThat(bigDecimal).isEqualTo(new BigDecimal(bigDecimalAsString));
	}

	@Test
	void parseLocalizedBigDecimalNumber3() {
		String bigDecimalAsString = "3.14159265358979323846";
		NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
		Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class, numberFormat);
		assertThat(bigDecimal).isEqualTo(new BigDecimal(bigDecimalAsString));
	}

	@Test
	void parseOverflow() {
		String aLong = "" + Long.MAX_VALUE;
		String aDouble = "" + Double.MAX_VALUE;

		assertThatIllegalArgumentException().isThrownBy(() ->
				NumberUtils.parseNumber(aLong, Byte.class));

		assertThatIllegalArgumentException().isThrownBy(() ->
				NumberUtils.parseNumber(aLong, Short.class));

		assertThatIllegalArgumentException().isThrownBy(() ->
				NumberUtils.parseNumber(aLong, Integer.class));

		assertThat(NumberUtils.parseNumber(aLong, Long.class)).isEqualTo(Long.valueOf(Long.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aDouble, Double.class)).isEqualTo(Double.valueOf(Double.MAX_VALUE));
	}

	@Test
	void parseNegativeOverflow() {
		String aLong = "" + Long.MIN_VALUE;
		String aDouble = "" + Double.MIN_VALUE;

		assertThatIllegalArgumentException().isThrownBy(() ->
				NumberUtils.parseNumber(aLong, Byte.class));

		assertThatIllegalArgumentException().isThrownBy(() ->
				NumberUtils.parseNumber(aLong, Short.class));

		assertThatIllegalArgumentException().isThrownBy(() ->
				NumberUtils.parseNumber(aLong, Integer.class));

		assertThat(NumberUtils.parseNumber(aLong, Long.class)).isEqualTo(Long.valueOf(Long.MIN_VALUE));
		assertThat(NumberUtils.parseNumber(aDouble, Double.class)).isEqualTo(Double.valueOf(Double.MIN_VALUE));
	}

	@Test
	void parseOverflowUsingNumberFormat() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		String aLong = "" + Long.MAX_VALUE;
		String aDouble = "" + Double.MAX_VALUE;

		assertThatIllegalArgumentException().isThrownBy(() ->
				NumberUtils.parseNumber(aLong, Byte.class, nf));

		assertThatIllegalArgumentException().isThrownBy(() ->
				NumberUtils.parseNumber(aLong, Short.class, nf));

		assertThatIllegalArgumentException().isThrownBy(() ->
				NumberUtils.parseNumber(aLong, Integer.class, nf));

		assertThat(NumberUtils.parseNumber(aLong, Long.class, nf)).isEqualTo(Long.valueOf(Long.MAX_VALUE));
		assertThat(NumberUtils.parseNumber(aDouble, Double.class, nf)).isEqualTo(Double.valueOf(Double.MAX_VALUE));
	}

	@Test
	void parseNegativeOverflowUsingNumberFormat() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		String aLong = "" + Long.MIN_VALUE;
		String aDouble = "" + Double.MIN_VALUE;

		assertThatIllegalArgumentException().isThrownBy(() ->
				NumberUtils.parseNumber(aLong, Byte.class, nf));

		assertThatIllegalArgumentException().isThrownBy(() ->
				NumberUtils.parseNumber(aLong, Short.class, nf));

		assertThatIllegalArgumentException().isThrownBy(() ->
				NumberUtils.parseNumber(aLong, Integer.class, nf));

		assertThat(NumberUtils.parseNumber(aLong, Long.class, nf)).isEqualTo(Long.valueOf(Long.MIN_VALUE));
		assertThat(NumberUtils.parseNumber(aDouble, Double.class, nf)).isEqualTo(Double.valueOf(Double.MIN_VALUE));
	}

	@Test
	void convertToInteger() {
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(-1), Integer.class)).isEqualTo(Integer.valueOf(-1));
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(0), Integer.class)).isEqualTo(Integer.valueOf(0));
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(1), Integer.class)).isEqualTo(Integer.valueOf(1));
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Integer.MAX_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Integer.MAX_VALUE + 1), Integer.class)).isEqualTo(Integer.valueOf(Integer.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Integer.MIN_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Integer.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Integer.MIN_VALUE - 1), Integer.class)).isEqualTo(Integer.valueOf(Integer.MAX_VALUE));

		assertThat(NumberUtils.convertNumberToTargetClass((long) -1, Integer.class)).isEqualTo(Integer.valueOf(-1));
		assertThat(NumberUtils.convertNumberToTargetClass(0L, Integer.class)).isEqualTo(Integer.valueOf(0));
		assertThat(NumberUtils.convertNumberToTargetClass(1L, Integer.class)).isEqualTo(Integer.valueOf(1));
		assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(Integer.MAX_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass((long) (Integer.MAX_VALUE + 1), Integer.class)).isEqualTo(Integer.valueOf(Integer.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass((long) Integer.MIN_VALUE, Integer.class)).isEqualTo(Integer.valueOf(Integer.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass((long) (Integer.MIN_VALUE - 1), Integer.class)).isEqualTo(Integer.valueOf(Integer.MAX_VALUE));

		assertThat(NumberUtils.convertNumberToTargetClass(-1, Integer.class)).isEqualTo(Integer.valueOf(-1));
		assertThat(NumberUtils.convertNumberToTargetClass(0, Integer.class)).isEqualTo(Integer.valueOf(0));
		assertThat(NumberUtils.convertNumberToTargetClass(1, Integer.class)).isEqualTo(Integer.valueOf(1));
		assertThat(NumberUtils.convertNumberToTargetClass(Integer.MAX_VALUE, Integer.class)).isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Integer.MAX_VALUE + 1, Integer.class)).isEqualTo(Integer.valueOf(Integer.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Integer.MIN_VALUE, Integer.class)).isEqualTo(Integer.valueOf(Integer.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Integer.MIN_VALUE - 1, Integer.class)).isEqualTo(Integer.valueOf(Integer.MAX_VALUE));

		assertThat(NumberUtils.convertNumberToTargetClass((short) -1, Integer.class)).isEqualTo(Integer.valueOf(-1));
		assertThat(NumberUtils.convertNumberToTargetClass((short) 0, Integer.class)).isEqualTo(Integer.valueOf(0));
		assertThat(NumberUtils.convertNumberToTargetClass((short) 1, Integer.class)).isEqualTo(Integer.valueOf(1));
		assertThat(NumberUtils.convertNumberToTargetClass(Short.MAX_VALUE, Integer.class)).isEqualTo(Integer.valueOf(Short.MAX_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass((short) (Short.MAX_VALUE + 1), Integer.class)).isEqualTo(Integer.valueOf(Short.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Short.MIN_VALUE, Integer.class)).isEqualTo(Integer.valueOf(Short.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass((short) (Short.MIN_VALUE - 1), Integer.class)).isEqualTo(Integer.valueOf(Short.MAX_VALUE));

		assertThat(NumberUtils.convertNumberToTargetClass((byte) -1, Integer.class)).isEqualTo(Integer.valueOf(-1));
		assertThat(NumberUtils.convertNumberToTargetClass((byte) 0, Integer.class)).isEqualTo(Integer.valueOf(0));
		assertThat(NumberUtils.convertNumberToTargetClass((byte) 1, Integer.class)).isEqualTo(Integer.valueOf(1));
		assertThat(NumberUtils.convertNumberToTargetClass(Byte.MAX_VALUE, Integer.class)).isEqualTo(Integer.valueOf(Byte.MAX_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass((byte) (Byte.MAX_VALUE + 1), Integer.class)).isEqualTo(Integer.valueOf(Byte.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Byte.MIN_VALUE, Integer.class)).isEqualTo(Integer.valueOf(Byte.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass((byte) (Byte.MIN_VALUE - 1), Integer.class)).isEqualTo(Integer.valueOf(Byte.MAX_VALUE));

		assertToNumberOverflow(Long.MAX_VALUE + 1, Integer.class);
		assertToNumberOverflow(Long.MIN_VALUE - 1, Integer.class);
		assertToNumberOverflow(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE), Integer.class);
		assertToNumberOverflow(BigInteger.valueOf(Integer.MIN_VALUE).subtract(BigInteger.ONE), Integer.class);
		assertToNumberOverflow(new BigDecimal("18446744073709551611"), Integer.class);
	}

	@Test
	void convertToLong() {
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(-1), Long.class)).isEqualTo(Long.valueOf(-1));
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(0), Long.class)).isEqualTo(Long.valueOf(0));
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(1), Long.class)).isEqualTo(Long.valueOf(1));
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Long.MAX_VALUE), Long.class)).isEqualTo(Long.valueOf(Long.MAX_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Long.MAX_VALUE + 1), Long.class)).isEqualTo(Long.valueOf(Long.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Long.MIN_VALUE), Long.class)).isEqualTo(Long.valueOf(Long.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Long.MIN_VALUE - 1), Long.class)).isEqualTo(Long.valueOf(Long.MAX_VALUE));

		assertThat(NumberUtils.convertNumberToTargetClass((long) -1, Long.class)).isEqualTo(Long.valueOf(-1));
		assertThat(NumberUtils.convertNumberToTargetClass(0L, Long.class)).isEqualTo(Long.valueOf(0));
		assertThat(NumberUtils.convertNumberToTargetClass(1L, Long.class)).isEqualTo(Long.valueOf(1));
		assertThat(NumberUtils.convertNumberToTargetClass(Long.MAX_VALUE, Long.class)).isEqualTo(Long.valueOf(Long.MAX_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Long.MAX_VALUE + 1, Long.class)).isEqualTo(Long.valueOf(Long.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Long.MIN_VALUE, Long.class)).isEqualTo(Long.valueOf(Long.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Long.MIN_VALUE - 1, Long.class)).isEqualTo(Long.valueOf(Long.MAX_VALUE));

		assertThat(NumberUtils.convertNumberToTargetClass(-1, Long.class)).isEqualTo(Long.valueOf(-1));
		assertThat(NumberUtils.convertNumberToTargetClass(0, Long.class)).isEqualTo(Long.valueOf(0));
		assertThat(NumberUtils.convertNumberToTargetClass(1, Long.class)).isEqualTo(Long.valueOf(1));
		assertThat(NumberUtils.convertNumberToTargetClass(Integer.MAX_VALUE, Long.class)).isEqualTo(Long.valueOf(Integer.MAX_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Integer.MAX_VALUE + 1, Long.class)).isEqualTo(Long.valueOf(Integer.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Integer.MIN_VALUE, Long.class)).isEqualTo(Long.valueOf(Integer.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Integer.MIN_VALUE - 1, Long.class)).isEqualTo(Long.valueOf(Integer.MAX_VALUE));

		assertThat(NumberUtils.convertNumberToTargetClass((short) -1, Long.class)).isEqualTo(Long.valueOf(-1));
		assertThat(NumberUtils.convertNumberToTargetClass((short) 0, Long.class)).isEqualTo(Long.valueOf(0));
		assertThat(NumberUtils.convertNumberToTargetClass((short) 1, Long.class)).isEqualTo(Long.valueOf(1));
		assertThat(NumberUtils.convertNumberToTargetClass(Short.MAX_VALUE, Long.class)).isEqualTo(Long.valueOf(Short.MAX_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass((short) (Short.MAX_VALUE + 1), Long.class)).isEqualTo(Long.valueOf(Short.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Short.MIN_VALUE, Long.class)).isEqualTo(Long.valueOf(Short.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass((short) (Short.MIN_VALUE - 1), Long.class)).isEqualTo(Long.valueOf(Short.MAX_VALUE));

		assertThat(NumberUtils.convertNumberToTargetClass((byte) -1, Long.class)).isEqualTo(Long.valueOf(-1));
		assertThat(NumberUtils.convertNumberToTargetClass((byte) 0, Long.class)).isEqualTo(Long.valueOf(0));
		assertThat(NumberUtils.convertNumberToTargetClass((byte) 1, Long.class)).isEqualTo(Long.valueOf(1));
		assertThat(NumberUtils.convertNumberToTargetClass(Byte.MAX_VALUE, Long.class)).isEqualTo(Long.valueOf(Byte.MAX_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass((byte) (Byte.MAX_VALUE + 1), Long.class)).isEqualTo(Long.valueOf(Byte.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass(Byte.MIN_VALUE, Long.class)).isEqualTo(Long.valueOf(Byte.MIN_VALUE));
		assertThat(NumberUtils.convertNumberToTargetClass((byte) (Byte.MIN_VALUE - 1), Long.class)).isEqualTo(Long.valueOf(Byte.MAX_VALUE));

		assertToNumberOverflow(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), Long.class);
		assertToNumberOverflow(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE), Long.class);
		assertToNumberOverflow(new BigDecimal("18446744073709551611"), Long.class);
	}


	private void assertLongEquals(String aLong) {
		assertThat(NumberUtils.parseNumber(aLong, Long.class)).as("Long did not parse").isEqualTo(Long.MAX_VALUE);
	}

	private void assertIntegerEquals(String anInteger) {
		assertThat(NumberUtils.parseNumber(anInteger, Integer.class)).as("Integer did not parse").isEqualTo(Integer.MAX_VALUE);
	}

	private void assertShortEquals(String aShort) {
		assertThat(NumberUtils.parseNumber(aShort, Short.class)).as("Short did not parse").isEqualTo(Short.MAX_VALUE);
	}

	private void assertByteEquals(String aByte) {
		assertThat(NumberUtils.parseNumber(aByte, Byte.class)).as("Byte did not parse").isEqualTo(Byte.MAX_VALUE);
	}

	private void assertNegativeLongEquals(String aLong) {
		assertThat(NumberUtils.parseNumber(aLong, Long.class)).as("Long did not parse").isEqualTo(Long.MIN_VALUE);
	}

	private void assertNegativeIntegerEquals(String anInteger) {
		assertThat(NumberUtils.parseNumber(anInteger, Integer.class)).as("Integer did not parse").isEqualTo(Integer.MIN_VALUE);
	}

	private void assertNegativeShortEquals(String aShort) {
		assertThat(NumberUtils.parseNumber(aShort, Short.class)).as("Short did not parse").isEqualTo(Short.MIN_VALUE);
	}

	private void assertNegativeByteEquals(String aByte) {
		assertThat(NumberUtils.parseNumber(aByte, Byte.class)).as("Byte did not parse").isEqualTo(Byte.MIN_VALUE);
	}

	private void assertToNumberOverflow(Number number, Class<? extends Number> targetClass) {
		String msg = "overflow: from=" + number + ", toClass=" + targetClass;
		assertThatIllegalArgumentException().as(msg).isThrownBy(() ->
				NumberUtils.convertNumberToTargetClass(number, targetClass))
			.withMessageEndingWith("overflow");
	}

}
