/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.util.ObjectUtils.*;

/**
 * Unit tests for {@link ObjectUtils}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Sam Brannen
 */
public class ObjectUtilsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void isCheckedException() {
		assertTrue(ObjectUtils.isCheckedException(new Exception()));
		assertTrue(ObjectUtils.isCheckedException(new SQLException()));

		assertFalse(ObjectUtils.isCheckedException(new RuntimeException()));
		assertFalse(ObjectUtils.isCheckedException(new IllegalArgumentException("")));

		// Any Throwable other than RuntimeException and Error
		// has to be considered checked according to the JLS.
		assertTrue(ObjectUtils.isCheckedException(new Throwable()));
	}

	@Test
	public void isCompatibleWithThrowsClause() {
		Class<?>[] empty = new Class<?>[0];
		Class<?>[] exception = new Class<?>[] {Exception.class};
		Class<?>[] sqlAndIO = new Class<?>[] {SQLException.class, IOException.class};
		Class<?>[] throwable = new Class<?>[] {Throwable.class};

		assertTrue(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException()));
		assertTrue(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException(), empty));
		assertTrue(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException(), exception));
		assertTrue(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException(), sqlAndIO));
		assertTrue(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException(), throwable));

		assertFalse(ObjectUtils.isCompatibleWithThrowsClause(new Exception()));
		assertFalse(ObjectUtils.isCompatibleWithThrowsClause(new Exception(), empty));
		assertTrue(ObjectUtils.isCompatibleWithThrowsClause(new Exception(), exception));
		assertFalse(ObjectUtils.isCompatibleWithThrowsClause(new Exception(), sqlAndIO));
		assertTrue(ObjectUtils.isCompatibleWithThrowsClause(new Exception(), throwable));

		assertFalse(ObjectUtils.isCompatibleWithThrowsClause(new SQLException()));
		assertFalse(ObjectUtils.isCompatibleWithThrowsClause(new SQLException(), empty));
		assertTrue(ObjectUtils.isCompatibleWithThrowsClause(new SQLException(), exception));
		assertTrue(ObjectUtils.isCompatibleWithThrowsClause(new SQLException(), sqlAndIO));
		assertTrue(ObjectUtils.isCompatibleWithThrowsClause(new SQLException(), throwable));

		assertFalse(ObjectUtils.isCompatibleWithThrowsClause(new Throwable()));
		assertFalse(ObjectUtils.isCompatibleWithThrowsClause(new Throwable(), empty));
		assertFalse(ObjectUtils.isCompatibleWithThrowsClause(new Throwable(), exception));
		assertFalse(ObjectUtils.isCompatibleWithThrowsClause(new Throwable(), sqlAndIO));
		assertTrue(ObjectUtils.isCompatibleWithThrowsClause(new Throwable(), throwable));
	}

	@Test
	public void isEmptyNull() {
		assertTrue(isEmpty(null));
	}

	@Test
	public void isEmptyArray() {
		assertTrue(isEmpty(new char[0]));
		assertTrue(isEmpty(new Object[0]));
		assertTrue(isEmpty(new Integer[0]));

		assertFalse(isEmpty(new int[] {42}));
		assertFalse(isEmpty(new Integer[] {42}));
	}

	@Test
	public void isEmptyCollection() {
		assertTrue(isEmpty(Collections.emptyList()));
		assertTrue(isEmpty(Collections.emptySet()));

		Set<String> set = new HashSet<>();
		set.add("foo");
		assertFalse(isEmpty(set));
		assertFalse(isEmpty(Arrays.asList("foo")));
	}

	@Test
	public void isEmptyMap() {
		assertTrue(isEmpty(Collections.emptyMap()));

		HashMap<String, Object> map = new HashMap<>();
		map.put("foo", 42L);
		assertFalse(isEmpty(map));
	}

	@Test
	public void isEmptyCharSequence() {
		assertTrue(isEmpty(new StringBuilder()));
		assertTrue(isEmpty(""));

		assertFalse(isEmpty(new StringBuilder("foo")));
		assertFalse(isEmpty("   "));
		assertFalse(isEmpty("\t"));
		assertFalse(isEmpty("foo"));
	}

	@Test
	public void isEmptyUnsupportedObjectType() {
		assertFalse(isEmpty(42L));
		assertFalse(isEmpty(new Object()));
	}

	@Test
	public void toObjectArray() {
		int[] a = new int[] {1, 2, 3, 4, 5};
		Integer[] wrapper = (Integer[]) ObjectUtils.toObjectArray(a);
		assertTrue(wrapper.length == 5);
		for (int i = 0; i < wrapper.length; i++) {
			assertEquals(a[i], wrapper[i].intValue());
		}
	}

	@Test
	public void toObjectArrayWithNull() {
		Object[] objects = ObjectUtils.toObjectArray(null);
		assertNotNull(objects);
		assertEquals(0, objects.length);
	}

	@Test
	public void toObjectArrayWithEmptyPrimitiveArray() {
		Object[] objects = ObjectUtils.toObjectArray(new byte[] {});
		assertNotNull(objects);
		assertEquals(0, objects.length);
	}

	@Test
	public void toObjectArrayWithNonArrayType() {
		exception.expect(IllegalArgumentException.class);
		ObjectUtils.toObjectArray("Not an []");
	}

	@Test
	public void toObjectArrayWithNonPrimitiveArray() {
		String[] source = new String[] {"Bingo"};
		assertArrayEquals(source, ObjectUtils.toObjectArray(source));
	}

	@Test
	public void addObjectToArraySunnyDay() {
		String[] array = new String[] {"foo", "bar"};
		String newElement = "baz";
		Object[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertEquals(3, newArray.length);
		assertEquals(newElement, newArray[2]);
	}

	@Test
	public void addObjectToArrayWhenEmpty() {
		String[] array = new String[0];
		String newElement = "foo";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertEquals(1, newArray.length);
		assertEquals(newElement, newArray[0]);
	}

	@Test
	public void addObjectToSingleNonNullElementArray() {
		String existingElement = "foo";
		String[] array = new String[] {existingElement};
		String newElement = "bar";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertEquals(2, newArray.length);
		assertEquals(existingElement, newArray[0]);
		assertEquals(newElement, newArray[1]);
	}

	@Test
	public void addObjectToSingleNullElementArray() {
		String[] array = new String[] {null};
		String newElement = "bar";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertEquals(2, newArray.length);
		assertEquals(null, newArray[0]);
		assertEquals(newElement, newArray[1]);
	}

	@Test
	public void addObjectToNullArray() throws Exception {
		String newElement = "foo";
		String[] newArray = ObjectUtils.addObjectToArray(null, newElement);
		assertEquals(1, newArray.length);
		assertEquals(newElement, newArray[0]);
	}

	@Test
	public void addNullObjectToNullArray() throws Exception {
		Object[] newArray = ObjectUtils.addObjectToArray(null, null);
		assertEquals(1, newArray.length);
		assertEquals(null, newArray[0]);
	}

	@Test
	public void nullSafeEqualsWithArrays() throws Exception {
		assertTrue(ObjectUtils.nullSafeEquals(new String[] {"a", "b", "c"}, new String[] {"a", "b", "c"}));
		assertTrue(ObjectUtils.nullSafeEquals(new int[] {1, 2, 3}, new int[] {1, 2, 3}));
	}

	@Test
	@Deprecated
	public void hashCodeWithBooleanFalse() {
		int expected = Boolean.FALSE.hashCode();
		assertEquals(expected, ObjectUtils.hashCode(false));
	}

	@Test
	@Deprecated
	public void hashCodeWithBooleanTrue() {
		int expected = Boolean.TRUE.hashCode();
		assertEquals(expected, ObjectUtils.hashCode(true));
	}

	@Test
	@Deprecated
	public void hashCodeWithDouble() {
		double dbl = 9830.43;
		int expected = (new Double(dbl)).hashCode();
		assertEquals(expected, ObjectUtils.hashCode(dbl));
	}

	@Test
	@Deprecated
	public void hashCodeWithFloat() {
		float flt = 34.8f;
		int expected = (new Float(flt)).hashCode();
		assertEquals(expected, ObjectUtils.hashCode(flt));
	}

	@Test
	@Deprecated
	public void hashCodeWithLong() {
		long lng = 883L;
		int expected = (new Long(lng)).hashCode();
		assertEquals(expected, ObjectUtils.hashCode(lng));
	}

	@Test
	public void identityToString() {
		Object obj = new Object();
		String expected = obj.getClass().getName() + "@" + ObjectUtils.getIdentityHexString(obj);
		String actual = ObjectUtils.identityToString(obj);
		assertEquals(expected, actual);
	}

	@Test
	public void identityToStringWithNullObject() {
		assertEquals("", ObjectUtils.identityToString(null));
	}

	@Test
	public void isArrayOfPrimitivesWithBooleanArray() {
		assertTrue(ClassUtils.isPrimitiveArray(boolean[].class));
	}

	@Test
	public void isArrayOfPrimitivesWithObjectArray() {
		assertFalse(ClassUtils.isPrimitiveArray(Object[].class));
	}

	@Test
	public void isArrayOfPrimitivesWithNonArray() {
		assertFalse(ClassUtils.isPrimitiveArray(String.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithBooleanPrimitiveClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(boolean.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithBooleanWrapperClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Boolean.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithBytePrimitiveClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(byte.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithByteWrapperClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Byte.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithCharacterClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Character.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithCharClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(char.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithDoublePrimitiveClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(double.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithDoubleWrapperClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Double.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithFloatPrimitiveClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(float.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithFloatWrapperClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Float.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithIntClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(int.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithIntegerClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Integer.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithLongPrimitiveClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(long.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithLongWrapperClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Long.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithNonPrimitiveOrWrapperClass() {
		assertFalse(ClassUtils.isPrimitiveOrWrapper(Object.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithShortPrimitiveClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(short.class));
	}

	@Test
	public void isPrimitiveOrWrapperWithShortWrapperClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Short.class));
	}

	@Test
	public void nullSafeHashCodeWithBooleanArray() {
		int expected = 31 * 7 + Boolean.TRUE.hashCode();
		expected = 31 * expected + Boolean.FALSE.hashCode();

		boolean[] array = {true, false};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithBooleanArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((boolean[]) null));
	}

	@Test
	public void nullSafeHashCodeWithByteArray() {
		int expected = 31 * 7 + 8;
		expected = 31 * expected + 10;

		byte[] array = {8, 10};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithByteArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((byte[]) null));
	}

	@Test
	public void nullSafeHashCodeWithCharArray() {
		int expected = 31 * 7 + 'a';
		expected = 31 * expected + 'E';

		char[] array = {'a', 'E'};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithCharArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((char[]) null));
	}

	@Test
	public void nullSafeHashCodeWithDoubleArray() {
		long bits = Double.doubleToLongBits(8449.65);
		int expected = 31 * 7 + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(9944.923);
		expected = 31 * expected + (int) (bits ^ (bits >>> 32));

		double[] array = {8449.65, 9944.923};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithDoubleArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((double[]) null));
	}

	@Test
	public void nullSafeHashCodeWithFloatArray() {
		int expected = 31 * 7 + Float.floatToIntBits(9.6f);
		expected = 31 * expected + Float.floatToIntBits(7.4f);

		float[] array = {9.6f, 7.4f};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithFloatArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((float[]) null));
	}

	@Test
	public void nullSafeHashCodeWithIntArray() {
		int expected = 31 * 7 + 884;
		expected = 31 * expected + 340;

		int[] array = {884, 340};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithIntArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((int[]) null));
	}

	@Test
	public void nullSafeHashCodeWithLongArray() {
		long lng = 7993L;
		int expected = 31 * 7 + (int) (lng ^ (lng >>> 32));
		lng = 84320L;
		expected = 31 * expected + (int) (lng ^ (lng >>> 32));

		long[] array = {7993L, 84320L};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithLongArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((long[]) null));
	}

	@Test
	public void nullSafeHashCodeWithObject() {
		String str = "Luke";
		assertEquals(str.hashCode(), ObjectUtils.nullSafeHashCode(str));
	}

	@Test
	public void nullSafeHashCodeWithObjectArray() {
		int expected = 31 * 7 + "Leia".hashCode();
		expected = 31 * expected + "Han".hashCode();

		Object[] array = {"Leia", "Han"};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithObjectArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((Object[]) null));
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingBooleanArray() {
		Object array = new boolean[] {true, false};
		int expected = ObjectUtils.nullSafeHashCode((boolean[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingByteArray() {
		Object array = new byte[] {6, 39};
		int expected = ObjectUtils.nullSafeHashCode((byte[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingCharArray() {
		Object array = new char[] {'l', 'M'};
		int expected = ObjectUtils.nullSafeHashCode((char[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingDoubleArray() {
		Object array = new double[] {68930.993, 9022.009};
		int expected = ObjectUtils.nullSafeHashCode((double[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingFloatArray() {
		Object array = new float[] {9.9f, 9.54f};
		int expected = ObjectUtils.nullSafeHashCode((float[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingIntArray() {
		Object array = new int[] {89, 32};
		int expected = ObjectUtils.nullSafeHashCode((int[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingLongArray() {
		Object array = new long[] {4389, 320};
		int expected = ObjectUtils.nullSafeHashCode((long[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingObjectArray() {
		Object array = new Object[] {"Luke", "Anakin"};
		int expected = ObjectUtils.nullSafeHashCode((Object[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingShortArray() {
		Object array = new short[] {5, 3};
		int expected = ObjectUtils.nullSafeHashCode((short[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((Object) null));
	}

	@Test
	public void nullSafeHashCodeWithShortArray() {
		int expected = 31 * 7 + 70;
		expected = 31 * expected + 8;

		short[] array = {70, 8};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithShortArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((short[]) null));
	}

	@Test
	public void nullSafeToStringWithBooleanArray() {
		boolean[] array = {true, false};
		assertEquals("{true, false}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithBooleanArrayBeingEmpty() {
		boolean[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithBooleanArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((boolean[]) null));
	}

	@Test
	public void nullSafeToStringWithByteArray() {
		byte[] array = {5, 8};
		assertEquals("{5, 8}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithByteArrayBeingEmpty() {
		byte[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithByteArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((byte[]) null));
	}

	@Test
	public void nullSafeToStringWithCharArray() {
		char[] array = {'A', 'B'};
		assertEquals("{'A', 'B'}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithCharArrayBeingEmpty() {
		char[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithCharArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((char[]) null));
	}

	@Test
	public void nullSafeToStringWithDoubleArray() {
		double[] array = {8594.93, 8594023.95};
		assertEquals("{8594.93, 8594023.95}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithDoubleArrayBeingEmpty() {
		double[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithDoubleArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((double[]) null));
	}

	@Test
	public void nullSafeToStringWithFloatArray() {
		float[] array = {8.6f, 43.8f};
		assertEquals("{8.6, 43.8}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithFloatArrayBeingEmpty() {
		float[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithFloatArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((float[]) null));
	}

	@Test
	public void nullSafeToStringWithIntArray() {
		int[] array = {9, 64};
		assertEquals("{9, 64}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithIntArrayBeingEmpty() {
		int[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithIntArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((int[]) null));
	}

	@Test
	public void nullSafeToStringWithLongArray() {
		long[] array = {434L, 23423L};
		assertEquals("{434, 23423}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithLongArrayBeingEmpty() {
		long[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithLongArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((long[]) null));
	}

	@Test
	public void nullSafeToStringWithPlainOldString() {
		assertEquals("I shoh love tha taste of mangoes", ObjectUtils.nullSafeToString("I shoh love tha taste of mangoes"));
	}

	@Test
	public void nullSafeToStringWithObjectArray() {
		Object[] array = {"Han", Long.valueOf(43)};
		assertEquals("{Han, 43}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithObjectArrayBeingEmpty() {
		Object[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithObjectArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((Object[]) null));
	}

	@Test
	public void nullSafeToStringWithShortArray() {
		short[] array = {7, 9};
		assertEquals("{7, 9}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithShortArrayBeingEmpty() {
		short[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithShortArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((short[]) null));
	}

	@Test
	public void nullSafeToStringWithStringArray() {
		String[] array = {"Luke", "Anakin"};
		assertEquals("{Luke, Anakin}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithStringArrayBeingEmpty() {
		String[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	@Test
	public void nullSafeToStringWithStringArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((String[]) null));
	}

	@Test
	public void containsConstant() {
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "FOO"), is(true));
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "foo"), is(true));
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "BaR"), is(true));
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "bar"), is(true));
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "BAZ"), is(true));
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "baz"), is(true));

		assertThat(ObjectUtils.containsConstant(Tropes.values(), "BOGUS"), is(false));

		assertThat(ObjectUtils.containsConstant(Tropes.values(), "FOO", true), is(true));
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "foo", true), is(false));
	}

	@Test
	public void caseInsensitiveValueOf() {
		assertThat(ObjectUtils.caseInsensitiveValueOf(Tropes.values(), "foo"), is(Tropes.FOO));
		assertThat(ObjectUtils.caseInsensitiveValueOf(Tropes.values(), "BAR"), is(Tropes.BAR));

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(
				is("Constant [bogus] does not exist in enum type org.springframework.util.ObjectUtilsTests$Tropes"));
		ObjectUtils.caseInsensitiveValueOf(Tropes.values(), "bogus");
	}

	private void assertEqualHashCodes(int expected, Object array) {
		int actual = ObjectUtils.nullSafeHashCode(array);
		assertEquals(expected, actual);
		assertTrue(array.hashCode() != actual);
	}


	enum Tropes {FOO, BAR, baz}

}
