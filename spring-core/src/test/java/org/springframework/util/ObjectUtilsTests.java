/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Unit tests for {@link ObjectUtils}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Sam Brannen
 */
class ObjectUtilsTests {

	@Test
	void isCheckedException() {
		assertThat(ObjectUtils.isCheckedException(new Exception())).isTrue();
		assertThat(ObjectUtils.isCheckedException(new SQLException())).isTrue();

		assertThat(ObjectUtils.isCheckedException(new RuntimeException())).isFalse();
		assertThat(ObjectUtils.isCheckedException(new IllegalArgumentException(""))).isFalse();

		// Any Throwable other than RuntimeException and Error
		// has to be considered checked according to the JLS.
		assertThat(ObjectUtils.isCheckedException(new Throwable())).isTrue();
	}

	@Test
	void isCompatibleWithThrowsClause() {
		Class<?>[] empty = new Class<?>[0];
		Class<?>[] exception = new Class<?>[] {Exception.class};
		Class<?>[] sqlAndIO = new Class<?>[] {SQLException.class, IOException.class};
		Class<?>[] throwable = new Class<?>[] {Throwable.class};

		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException())).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException(), empty)).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException(), exception)).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException(), sqlAndIO)).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException(), throwable)).isTrue();

		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Exception())).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Exception(), empty)).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Exception(), exception)).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Exception(), sqlAndIO)).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Exception(), throwable)).isTrue();

		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new SQLException())).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new SQLException(), empty)).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new SQLException(), exception)).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new SQLException(), sqlAndIO)).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new SQLException(), throwable)).isTrue();

		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Throwable())).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Throwable(), empty)).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Throwable(), exception)).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Throwable(), sqlAndIO)).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Throwable(), throwable)).isTrue();
	}

	@Test
	void isEmptyNull() {
		assertThat(isEmpty(null)).isTrue();
	}

	@Test
	void isEmptyArray() {
		assertThat(isEmpty(new char[0])).isTrue();
		assertThat(isEmpty(new Object[0])).isTrue();
		assertThat(isEmpty(new Integer[0])).isTrue();

		assertThat(isEmpty(new int[] {42})).isFalse();
		assertThat(isEmpty(new Integer[] {42})).isFalse();
	}

	@Test
	void isEmptyCollection() {
		assertThat(isEmpty(Collections.emptyList())).isTrue();
		assertThat(isEmpty(Collections.emptySet())).isTrue();

		Set<String> set = new HashSet<>();
		set.add("foo");
		assertThat(isEmpty(set)).isFalse();
		assertThat(isEmpty(Arrays.asList("foo"))).isFalse();
	}

	@Test
	void isEmptyMap() {
		assertThat(isEmpty(Collections.emptyMap())).isTrue();

		HashMap<String, Object> map = new HashMap<>();
		map.put("foo", 42L);
		assertThat(isEmpty(map)).isFalse();
	}

	@Test
	void isEmptyCharSequence() {
		assertThat(isEmpty(new StringBuilder())).isTrue();
		assertThat(isEmpty("")).isTrue();

		assertThat(isEmpty(new StringBuilder("foo"))).isFalse();
		assertThat(isEmpty("   ")).isFalse();
		assertThat(isEmpty("\t")).isFalse();
		assertThat(isEmpty("foo")).isFalse();
	}

	@Test
	void isEmptyUnsupportedObjectType() {
		assertThat(isEmpty(42L)).isFalse();
		assertThat(isEmpty(new Object())).isFalse();
	}

	@Test
	void toObjectArray() {
		int[] a = new int[] {1, 2, 3, 4, 5};
		Integer[] wrapper = (Integer[]) ObjectUtils.toObjectArray(a);
		assertThat(wrapper.length == 5).isTrue();
		for (int i = 0; i < wrapper.length; i++) {
			assertThat(wrapper[i].intValue()).isEqualTo(a[i]);
		}
	}

	@Test
	void toObjectArrayWithNull() {
		Object[] objects = ObjectUtils.toObjectArray(null);
		assertThat(objects).isNotNull();
		assertThat(objects.length).isEqualTo(0);
	}

	@Test
	void toObjectArrayWithEmptyPrimitiveArray() {
		Object[] objects = ObjectUtils.toObjectArray(new byte[] {});
		assertThat(objects).isNotNull();
		assertThat(objects.length).isEqualTo(0);
	}

	@Test
	void toObjectArrayWithNonArrayType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ObjectUtils.toObjectArray("Not an []"));
	}

	@Test
	void toObjectArrayWithNonPrimitiveArray() {
		String[] source = new String[] {"Bingo"};
		assertThat(ObjectUtils.toObjectArray(source)).isEqualTo(source);
	}

	@Test
	void addObjectToArraySunnyDay() {
		String[] array = new String[] {"foo", "bar"};
		String newElement = "baz";
		Object[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertThat(newArray.length).isEqualTo(3);
		assertThat(newArray[2]).isEqualTo(newElement);
	}

	@Test
	void addObjectToArrayWhenEmpty() {
		String[] array = new String[0];
		String newElement = "foo";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertThat(newArray.length).isEqualTo(1);
		assertThat(newArray[0]).isEqualTo(newElement);
	}

	@Test
	void addObjectToSingleNonNullElementArray() {
		String existingElement = "foo";
		String[] array = new String[] {existingElement};
		String newElement = "bar";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertThat(newArray.length).isEqualTo(2);
		assertThat(newArray[0]).isEqualTo(existingElement);
		assertThat(newArray[1]).isEqualTo(newElement);
	}

	@Test
	void addObjectToSingleNullElementArray() {
		String[] array = new String[] {null};
		String newElement = "bar";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertThat(newArray.length).isEqualTo(2);
		assertThat(newArray[0]).isEqualTo(null);
		assertThat(newArray[1]).isEqualTo(newElement);
	}

	@Test
	void addObjectToNullArray() throws Exception {
		String newElement = "foo";
		String[] newArray = ObjectUtils.addObjectToArray(null, newElement);
		assertThat(newArray.length).isEqualTo(1);
		assertThat(newArray[0]).isEqualTo(newElement);
	}

	@Test
	void addNullObjectToNullArray() throws Exception {
		Object[] newArray = ObjectUtils.addObjectToArray(null, null);
		assertThat(newArray.length).isEqualTo(1);
		assertThat(newArray[0]).isEqualTo(null);
	}

	@Test
	void nullSafeEqualsWithArrays() throws Exception {
		assertThat(ObjectUtils.nullSafeEquals(new String[] {"a", "b", "c"}, new String[] {"a", "b", "c"})).isTrue();
		assertThat(ObjectUtils.nullSafeEquals(new int[] {1, 2, 3}, new int[] {1, 2, 3})).isTrue();
	}

	@Test
	@Deprecated
	void hashCodeWithBooleanFalse() {
		int expected = Boolean.FALSE.hashCode();
		assertThat(ObjectUtils.hashCode(false)).isEqualTo(expected);
	}

	@Test
	@Deprecated
	void hashCodeWithBooleanTrue() {
		int expected = Boolean.TRUE.hashCode();
		assertThat(ObjectUtils.hashCode(true)).isEqualTo(expected);
	}

	@Test
	@Deprecated
	void hashCodeWithDouble() {
		double dbl = 9830.43;
		int expected = (new Double(dbl)).hashCode();
		assertThat(ObjectUtils.hashCode(dbl)).isEqualTo(expected);
	}

	@Test
	@Deprecated
	void hashCodeWithFloat() {
		float flt = 34.8f;
		int expected = (new Float(flt)).hashCode();
		assertThat(ObjectUtils.hashCode(flt)).isEqualTo(expected);
	}

	@Test
	@Deprecated
	void hashCodeWithLong() {
		long lng = 883L;
		int expected = (new Long(lng)).hashCode();
		assertThat(ObjectUtils.hashCode(lng)).isEqualTo(expected);
	}

	@Test
	void identityToString() {
		Object obj = new Object();
		String expected = obj.getClass().getName() + "@" + ObjectUtils.getIdentityHexString(obj);
		String actual = ObjectUtils.identityToString(obj);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void identityToStringWithNullObject() {
		assertThat(ObjectUtils.identityToString(null)).isEqualTo("");
	}

	@Test
	void isArrayOfPrimitivesWithBooleanArray() {
		assertThat(ClassUtils.isPrimitiveArray(boolean[].class)).isTrue();
	}

	@Test
	void isArrayOfPrimitivesWithObjectArray() {
		assertThat(ClassUtils.isPrimitiveArray(Object[].class)).isFalse();
	}

	@Test
	void isArrayOfPrimitivesWithNonArray() {
		assertThat(ClassUtils.isPrimitiveArray(String.class)).isFalse();
	}

	@Test
	void isPrimitiveOrWrapperWithBooleanPrimitiveClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(boolean.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithBooleanWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Boolean.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithBytePrimitiveClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(byte.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithByteWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Byte.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithCharacterClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Character.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithCharClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(char.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithDoublePrimitiveClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(double.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithDoubleWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Double.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithFloatPrimitiveClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(float.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithFloatWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Float.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithIntClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(int.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithIntegerClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Integer.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithLongPrimitiveClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(long.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithLongWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Long.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithNonPrimitiveOrWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Object.class)).isFalse();
	}

	@Test
	void isPrimitiveOrWrapperWithShortPrimitiveClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(short.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithShortWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Short.class)).isTrue();
	}

	@Test
	void nullSafeHashCodeWithBooleanArray() {
		int expected = 31 * 7 + Boolean.TRUE.hashCode();
		expected = 31 * expected + Boolean.FALSE.hashCode();

		boolean[] array = {true, false};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithBooleanArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((boolean[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithByteArray() {
		int expected = 31 * 7 + 8;
		expected = 31 * expected + 10;

		byte[] array = {8, 10};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithByteArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((byte[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithCharArray() {
		int expected = 31 * 7 + 'a';
		expected = 31 * expected + 'E';

		char[] array = {'a', 'E'};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithCharArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((char[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithDoubleArray() {
		long bits = Double.doubleToLongBits(8449.65);
		int expected = 31 * 7 + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(9944.923);
		expected = 31 * expected + (int) (bits ^ (bits >>> 32));

		double[] array = {8449.65, 9944.923};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithDoubleArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((double[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithFloatArray() {
		int expected = 31 * 7 + Float.floatToIntBits(9.6f);
		expected = 31 * expected + Float.floatToIntBits(7.4f);

		float[] array = {9.6f, 7.4f};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithFloatArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((float[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithIntArray() {
		int expected = 31 * 7 + 884;
		expected = 31 * expected + 340;

		int[] array = {884, 340};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithIntArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((int[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithLongArray() {
		long lng = 7993L;
		int expected = 31 * 7 + (int) (lng ^ (lng >>> 32));
		lng = 84320L;
		expected = 31 * expected + (int) (lng ^ (lng >>> 32));

		long[] array = {7993L, 84320L};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithLongArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((long[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithObject() {
		String str = "Luke";
		assertThat(ObjectUtils.nullSafeHashCode(str)).isEqualTo(str.hashCode());
	}

	@Test
	void nullSafeHashCodeWithObjectArray() {
		int expected = 31 * 7 + "Leia".hashCode();
		expected = 31 * expected + "Han".hashCode();

		Object[] array = {"Leia", "Han"};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithObjectArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((Object[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingBooleanArray() {
		Object array = new boolean[] {true, false};
		int expected = ObjectUtils.nullSafeHashCode((boolean[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingByteArray() {
		Object array = new byte[] {6, 39};
		int expected = ObjectUtils.nullSafeHashCode((byte[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingCharArray() {
		Object array = new char[] {'l', 'M'};
		int expected = ObjectUtils.nullSafeHashCode((char[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingDoubleArray() {
		Object array = new double[] {68930.993, 9022.009};
		int expected = ObjectUtils.nullSafeHashCode((double[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingFloatArray() {
		Object array = new float[] {9.9f, 9.54f};
		int expected = ObjectUtils.nullSafeHashCode((float[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingIntArray() {
		Object array = new int[] {89, 32};
		int expected = ObjectUtils.nullSafeHashCode((int[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingLongArray() {
		Object array = new long[] {4389, 320};
		int expected = ObjectUtils.nullSafeHashCode((long[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingObjectArray() {
		Object array = new Object[] {"Luke", "Anakin"};
		int expected = ObjectUtils.nullSafeHashCode((Object[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingShortArray() {
		Object array = new short[] {5, 3};
		int expected = ObjectUtils.nullSafeHashCode((short[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((Object) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithShortArray() {
		int expected = 31 * 7 + 70;
		expected = 31 * expected + 8;

		short[] array = {70, 8};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithShortArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((short[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeToStringWithBooleanArray() {
		boolean[] array = {true, false};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{true, false}");
	}

	@Test
	void nullSafeToStringWithBooleanArrayBeingEmpty() {
		boolean[] array = {};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithBooleanArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((boolean[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithByteArray() {
		byte[] array = {5, 8};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{5, 8}");
	}

	@Test
	void nullSafeToStringWithByteArrayBeingEmpty() {
		byte[] array = {};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithByteArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((byte[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithCharArray() {
		char[] array = {'A', 'B'};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{'A', 'B'}");
	}

	@Test
	void nullSafeToStringWithCharArrayBeingEmpty() {
		char[] array = {};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithCharArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((char[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithDoubleArray() {
		double[] array = {8594.93, 8594023.95};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{8594.93, 8594023.95}");
	}

	@Test
	void nullSafeToStringWithDoubleArrayBeingEmpty() {
		double[] array = {};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithDoubleArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((double[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithFloatArray() {
		float[] array = {8.6f, 43.8f};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{8.6, 43.8}");
	}

	@Test
	void nullSafeToStringWithFloatArrayBeingEmpty() {
		float[] array = {};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithFloatArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((float[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithIntArray() {
		int[] array = {9, 64};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{9, 64}");
	}

	@Test
	void nullSafeToStringWithIntArrayBeingEmpty() {
		int[] array = {};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithIntArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((int[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithLongArray() {
		long[] array = {434L, 23423L};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{434, 23423}");
	}

	@Test
	void nullSafeToStringWithLongArrayBeingEmpty() {
		long[] array = {};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithLongArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((long[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithPlainOldString() {
		assertThat(ObjectUtils.nullSafeToString("I shoh love tha taste of mangoes")).isEqualTo("I shoh love tha taste of mangoes");
	}

	@Test
	void nullSafeToStringWithObjectArray() {
		Object[] array = {"Han", Long.valueOf(43)};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{Han, 43}");
	}

	@Test
	void nullSafeToStringWithObjectArrayBeingEmpty() {
		Object[] array = {};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithObjectArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((Object[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithShortArray() {
		short[] array = {7, 9};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{7, 9}");
	}

	@Test
	void nullSafeToStringWithShortArrayBeingEmpty() {
		short[] array = {};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithShortArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((short[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithStringArray() {
		String[] array = {"Luke", "Anakin"};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{Luke, Anakin}");
	}

	@Test
	void nullSafeToStringWithStringArrayBeingEmpty() {
		String[] array = {};
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithStringArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((String[]) null)).isEqualTo("null");
	}

	@Test
	void containsConstant() {
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "FOO")).isTrue();
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "foo")).isTrue();
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "BaR")).isTrue();
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "bar")).isTrue();
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "BAZ")).isTrue();
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "baz")).isTrue();

		assertThat(ObjectUtils.containsConstant(Tropes.values(), "BOGUS")).isFalse();

		assertThat(ObjectUtils.containsConstant(Tropes.values(), "FOO", true)).isTrue();
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "foo", true)).isFalse();
	}

	@Test
	void caseInsensitiveValueOf() {
		assertThat(ObjectUtils.caseInsensitiveValueOf(Tropes.values(), "foo")).isEqualTo(Tropes.FOO);
		assertThat(ObjectUtils.caseInsensitiveValueOf(Tropes.values(), "BAR")).isEqualTo(Tropes.BAR);

		assertThatIllegalArgumentException().isThrownBy(() ->
				ObjectUtils.caseInsensitiveValueOf(Tropes.values(), "bogus"))
			.withMessage("Constant [bogus] does not exist in enum type org.springframework.util.ObjectUtilsTests$Tropes");
	}

	private void assertEqualHashCodes(int expected, Object array) {
		int actual = ObjectUtils.nullSafeHashCode(array);
		assertThat(actual).isEqualTo(expected);
		assertThat(array.hashCode() != actual).isTrue();
	}


	enum Tropes {FOO, BAR, baz}

}
