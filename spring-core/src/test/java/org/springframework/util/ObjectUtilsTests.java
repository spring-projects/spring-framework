/*
 * Copyright 2002-2013 the original author or authors.
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

import junit.framework.TestCase;

import org.springframework.core.task.TaskRejectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public final class ObjectUtilsTests extends TestCase {

	public void testIsCheckedException() {
		assertTrue(ObjectUtils.isCheckedException(new Exception()));
		assertTrue(ObjectUtils.isCheckedException(new SQLException()));

		assertFalse(ObjectUtils.isCheckedException(new RuntimeException()));
		assertFalse(ObjectUtils.isCheckedException(new TaskRejectedException("")));

		// Any Throwable other than RuntimeException and Error
		// has to be considered checked according to the JLS.
		assertTrue(ObjectUtils.isCheckedException(new Throwable()));
	}

	public void testIsCompatibleWithThrowsClause() {
		Class<?>[] empty = new Class[0];
		Class<?>[] exception = new Class[] {Exception.class};
		Class<?>[] sqlAndIO = new Class[] {SQLException.class, IOException.class};
		Class<?>[] throwable = new Class[] {Throwable.class};

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

	public void testToObjectArray() {
		int[] a = new int[] {1, 2, 3, 4, 5};
		Integer[] wrapper = (Integer[]) ObjectUtils.toObjectArray(a);
		assertTrue(wrapper.length == 5);
		for (int i = 0; i < wrapper.length; i++) {
			assertEquals(a[i], wrapper[i].intValue());
		}
	}

	public void testToObjectArrayWithNull() {
		Object[] objects = ObjectUtils.toObjectArray(null);
		assertNotNull(objects);
		assertEquals(0, objects.length);
	}

	public void testToObjectArrayWithEmptyPrimitiveArray() {
		Object[] objects = ObjectUtils.toObjectArray(new byte[] {});
		assertNotNull(objects);
		assertEquals(0, objects.length);
	}

	public void testToObjectArrayWithNonArrayType() {
		try {
			ObjectUtils.toObjectArray("Not an []");
			fail("Must have thrown an IllegalArgumentException by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testToObjectArrayWithNonPrimitiveArray() {
		String[] source = new String[] {"Bingo"};
		assertEquals(source, ObjectUtils.toObjectArray(source));
	}

	public void testAddObjectToArraySunnyDay() {
		String[] array = new String[] {"foo", "bar"};
		String newElement = "baz";
		Object[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertEquals(3, newArray.length);
		assertEquals(newElement, newArray[2]);
	}

	public void testAddObjectToArrayWhenEmpty() {
		String[] array = new String[0];
		String newElement = "foo";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertEquals(1, newArray.length);
		assertEquals(newElement, newArray[0]);
	}

	public void testAddObjectToSingleNonNullElementArray() {
		String existingElement = "foo";
		String[] array = new String[] {existingElement};
		String newElement = "bar";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertEquals(2, newArray.length);
		assertEquals(existingElement, newArray[0]);
		assertEquals(newElement, newArray[1]);
	}

	public void testAddObjectToSingleNullElementArray() {
		String[] array = new String[] {null};
		String newElement = "bar";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertEquals(2, newArray.length);
		assertEquals(null, newArray[0]);
		assertEquals(newElement, newArray[1]);
	}

	public void testAddObjectToNullArray() throws Exception {
		String newElement = "foo";
		String[] newArray = ObjectUtils.addObjectToArray(null, newElement);
		assertEquals(1, newArray.length);
		assertEquals(newElement, newArray[0]);
	}

	public void testAddNullObjectToNullArray() throws Exception {
		Object[] newArray = ObjectUtils.addObjectToArray(null, null);
		assertEquals(1, newArray.length);
		assertEquals(null, newArray[0]);
	}

	public void testNullSafeEqualsWithArrays() throws Exception {
		assertTrue(ObjectUtils.nullSafeEquals(new String[] {"a", "b", "c"}, new String[] {"a", "b", "c"}));
		assertTrue(ObjectUtils.nullSafeEquals(new int[] {1, 2, 3}, new int[] {1, 2, 3}));
	}

	public void testHashCodeWithBooleanFalse() {
		int expected = Boolean.FALSE.hashCode();
		assertEquals(expected, ObjectUtils.hashCode(false));
	}

	public void testHashCodeWithBooleanTrue() {
		int expected = Boolean.TRUE.hashCode();
		assertEquals(expected, ObjectUtils.hashCode(true));
	}

	public void testHashCodeWithDouble() {
		double dbl = 9830.43;
		int expected = (new Double(dbl)).hashCode();
		assertEquals(expected, ObjectUtils.hashCode(dbl));
	}

	public void testHashCodeWithFloat() {
		float flt = 34.8f;
		int expected = (new Float(flt)).hashCode();
		assertEquals(expected, ObjectUtils.hashCode(flt));
	}

	public void testHashCodeWithLong() {
		long lng = 883l;
		int expected = (new Long(lng)).hashCode();
		assertEquals(expected, ObjectUtils.hashCode(lng));
	}

	public void testIdentityToString() {
		Object obj = new Object();
		String expected = obj.getClass().getName() + "@" + ObjectUtils.getIdentityHexString(obj);
		String actual = ObjectUtils.identityToString(obj);
		assertEquals(expected, actual);
	}

	public void testIdentityToStringWithNullObject() {
		assertEquals("", ObjectUtils.identityToString(null));
	}

	public void testIsArrayOfPrimitivesWithBooleanArray() {
		assertTrue(ClassUtils.isPrimitiveArray(boolean[].class));
	}

	public void testIsArrayOfPrimitivesWithObjectArray() {
		assertFalse(ClassUtils.isPrimitiveArray(Object[].class));
	}

	public void testIsArrayOfPrimitivesWithNonArray() {
		assertFalse(ClassUtils.isPrimitiveArray(String.class));
	}

	public void testIsPrimitiveOrWrapperWithBooleanPrimitiveClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(boolean.class));
	}

	public void testIsPrimitiveOrWrapperWithBooleanWrapperClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Boolean.class));
	}

	public void testIsPrimitiveOrWrapperWithBytePrimitiveClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(byte.class));
	}

	public void testIsPrimitiveOrWrapperWithByteWrapperClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Byte.class));
	}

	public void testIsPrimitiveOrWrapperWithCharacterClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Character.class));
	}

	public void testIsPrimitiveOrWrapperWithCharClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(char.class));
	}

	public void testIsPrimitiveOrWrapperWithDoublePrimitiveClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(double.class));
	}

	public void testIsPrimitiveOrWrapperWithDoubleWrapperClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Double.class));
	}

	public void testIsPrimitiveOrWrapperWithFloatPrimitiveClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(float.class));
	}

	public void testIsPrimitiveOrWrapperWithFloatWrapperClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Float.class));
	}

	public void testIsPrimitiveOrWrapperWithIntClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(int.class));
	}

	public void testIsPrimitiveOrWrapperWithIntegerClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Integer.class));
	}

	public void testIsPrimitiveOrWrapperWithLongPrimitiveClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(long.class));
	}

	public void testIsPrimitiveOrWrapperWithLongWrapperClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Long.class));
	}

	public void testIsPrimitiveOrWrapperWithNonPrimitiveOrWrapperClass() {
		assertFalse(ClassUtils.isPrimitiveOrWrapper(Object.class));
	}

	public void testIsPrimitiveOrWrapperWithShortPrimitiveClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(short.class));
	}

	public void testIsPrimitiveOrWrapperWithShortWrapperClass() {
		assertTrue(ClassUtils.isPrimitiveOrWrapper(Short.class));
	}

	public void testNullSafeHashCodeWithBooleanArray() {
		int expected = 31 * 7 + Boolean.TRUE.hashCode();
		expected = 31 * expected + Boolean.FALSE.hashCode();

		boolean[] array = {true, false};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	public void testNullSafeHashCodeWithBooleanArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((boolean[]) null));
	}

	public void testNullSafeHashCodeWithByteArray() {
		int expected = 31 * 7 + 8;
		expected = 31 * expected + 10;

		byte[] array = {8, 10};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	public void testNullSafeHashCodeWithByteArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((byte[]) null));
	}

	public void testNullSafeHashCodeWithCharArray() {
		int expected = 31 * 7 + 'a';
		expected = 31 * expected + 'E';

		char[] array = {'a', 'E'};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	public void testNullSafeHashCodeWithCharArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((char[]) null));
	}

	public void testNullSafeHashCodeWithDoubleArray() {
		long bits = Double.doubleToLongBits(8449.65);
		int expected = 31 * 7 + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(9944.923);
		expected = 31 * expected + (int) (bits ^ (bits >>> 32));

		double[] array = {8449.65, 9944.923};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	public void testNullSafeHashCodeWithDoubleArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((double[]) null));
	}

	public void testNullSafeHashCodeWithFloatArray() {
		int expected = 31 * 7 + Float.floatToIntBits(9.6f);
		expected = 31 * expected + Float.floatToIntBits(7.4f);

		float[] array = {9.6f, 7.4f};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	public void testNullSafeHashCodeWithFloatArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((float[]) null));
	}

	public void testNullSafeHashCodeWithIntArray() {
		int expected = 31 * 7 + 884;
		expected = 31 * expected + 340;

		int[] array = {884, 340};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	public void testNullSafeHashCodeWithIntArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((int[]) null));
	}

	public void testNullSafeHashCodeWithLongArray() {
		long lng = 7993l;
		int expected = 31 * 7 + (int) (lng ^ (lng >>> 32));
		lng = 84320l;
		expected = 31 * expected + (int) (lng ^ (lng >>> 32));

		long[] array = {7993l, 84320l};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	public void testNullSafeHashCodeWithLongArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((long[]) null));
	}

	public void testNullSafeHashCodeWithObject() {
		String str = "Luke";
		assertEquals(str.hashCode(), ObjectUtils.nullSafeHashCode(str));
	}

	public void testNullSafeHashCodeWithObjectArray() {
		int expected = 31 * 7 + "Leia".hashCode();
		expected = 31 * expected + "Han".hashCode();

		Object[] array = {"Leia", "Han"};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	public void testNullSafeHashCodeWithObjectArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((Object[]) null));
	}

	public void testNullSafeHashCodeWithObjectBeingBooleanArray() {
		Object array = new boolean[] {true, false};
		int expected = ObjectUtils.nullSafeHashCode((boolean[]) array);
		assertEqualHashCodes(expected, array);
	}

	public void testNullSafeHashCodeWithObjectBeingByteArray() {
		Object array = new byte[] {6, 39};
		int expected = ObjectUtils.nullSafeHashCode((byte[]) array);
		assertEqualHashCodes(expected, array);
	}

	public void testNullSafeHashCodeWithObjectBeingCharArray() {
		Object array = new char[] {'l', 'M'};
		int expected = ObjectUtils.nullSafeHashCode((char[]) array);
		assertEqualHashCodes(expected, array);
	}

	public void testNullSafeHashCodeWithObjectBeingDoubleArray() {
		Object array = new double[] {68930.993, 9022.009};
		int expected = ObjectUtils.nullSafeHashCode((double[]) array);
		assertEqualHashCodes(expected, array);
	}

	public void testNullSafeHashCodeWithObjectBeingFloatArray() {
		Object array = new float[] {9.9f, 9.54f};
		int expected = ObjectUtils.nullSafeHashCode((float[]) array);
		assertEqualHashCodes(expected, array);
	}

	public void testNullSafeHashCodeWithObjectBeingIntArray() {
		Object array = new int[] {89, 32};
		int expected = ObjectUtils.nullSafeHashCode((int[]) array);
		assertEqualHashCodes(expected, array);
	}

	public void testNullSafeHashCodeWithObjectBeingLongArray() {
		Object array = new long[] {4389, 320};
		int expected = ObjectUtils.nullSafeHashCode((long[]) array);
		assertEqualHashCodes(expected, array);
	}

	public void testNullSafeHashCodeWithObjectBeingObjectArray() {
		Object array = new Object[] {"Luke", "Anakin"};
		int expected = ObjectUtils.nullSafeHashCode((Object[]) array);
		assertEqualHashCodes(expected, array);
	}

	public void testNullSafeHashCodeWithObjectBeingShortArray() {
		Object array = new short[] {5, 3};
		int expected = ObjectUtils.nullSafeHashCode((short[]) array);
		assertEqualHashCodes(expected, array);
	}

	public void testNullSafeHashCodeWithObjectEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((Object) null));
	}

	public void testNullSafeHashCodeWithShortArray() {
		int expected = 31 * 7 + 70;
		expected = 31 * expected + 8;

		short[] array = {70, 8};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	public void testNullSafeHashCodeWithShortArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((short[]) null));
	}

	public void testNullSafeToStringWithBooleanArray() {
		boolean[] array = {true, false};
		assertEquals("{true, false}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithBooleanArrayBeingEmpty() {
		boolean[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithBooleanArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((boolean[]) null));
	}

	public void testNullSafeToStringWithByteArray() {
		byte[] array = {5, 8};
		assertEquals("{5, 8}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithByteArrayBeingEmpty() {
		byte[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithByteArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((byte[]) null));
	}

	public void testNullSafeToStringWithCharArray() {
		char[] array = {'A', 'B'};
		assertEquals("{'A', 'B'}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithCharArrayBeingEmpty() {
		char[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithCharArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((char[]) null));
	}

	public void testNullSafeToStringWithDoubleArray() {
		double[] array = {8594.93, 8594023.95};
		assertEquals("{8594.93, 8594023.95}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithDoubleArrayBeingEmpty() {
		double[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithDoubleArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((double[]) null));
	}

	public void testNullSafeToStringWithFloatArray() {
		float[] array = {8.6f, 43.8f};
		assertEquals("{8.6, 43.8}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithFloatArrayBeingEmpty() {
		float[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithFloatArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((float[]) null));
	}

	public void testNullSafeToStringWithIntArray() {
		int[] array = {9, 64};
		assertEquals("{9, 64}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithIntArrayBeingEmpty() {
		int[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithIntArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((int[]) null));
	}

	public void testNullSafeToStringWithLongArray() {
		long[] array = {434l, 23423l};
		assertEquals("{434, 23423}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithLongArrayBeingEmpty() {
		long[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithLongArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((long[]) null));
	}

	public void testNullSafeToStringWithPlainOldString() {
		assertEquals("I shoh love tha taste of mangoes", ObjectUtils.nullSafeToString("I shoh love tha taste of mangoes"));
	}

	public void testNullSafeToStringWithObjectArray() {
		Object[] array = {"Han", new Long(43)};
		assertEquals("{Han, 43}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithObjectArrayBeingEmpty() {
		Object[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithObjectArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((Object[]) null));
	}

	public void testNullSafeToStringWithShortArray() {
		short[] array = {7, 9};
		assertEquals("{7, 9}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithShortArrayBeingEmpty() {
		short[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithShortArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((short[]) null));
	}

	public void testNullSafeToStringWithStringArray() {
		String[] array = {"Luke", "Anakin"};
		assertEquals("{Luke, Anakin}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithStringArrayBeingEmpty() {
		String[] array = {};
		assertEquals("{}", ObjectUtils.nullSafeToString(array));
	}

	public void testNullSafeToStringWithStringArrayEqualToNull() {
		assertEquals("null", ObjectUtils.nullSafeToString((String[]) null));
	}

	enum Tropes { FOO, BAR, baz }

	public void testContainsConstant() {
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

	public void testCaseInsensitiveValueOf() {
		assertThat(ObjectUtils.caseInsensitiveValueOf(Tropes.values(), "foo"), is(Tropes.FOO));
		assertThat(ObjectUtils.caseInsensitiveValueOf(Tropes.values(), "BAR"), is(Tropes.BAR));
		try {
			ObjectUtils.caseInsensitiveValueOf(Tropes.values(), "bogus");
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(),
					is("constant [bogus] does not exist in enum type " +
					"org.springframework.util.ObjectUtilsTests$Tropes"));
		}
	}

	private void assertEqualHashCodes(int expected, Object array) {
		int actual = ObjectUtils.nullSafeHashCode(array);
		assertEquals(expected, actual);
		assertTrue(array.hashCode() != actual);
	}

}
