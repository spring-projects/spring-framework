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

import java.io.File;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.lang.Nullable;

/**
 * Miscellaneous object utility methods.
 *
 * <p>Mainly for internal use within the framework.
 *
 * <p>Thanks to Alex Ruiz for contributing several enhancements to this class!
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sam Brannen
 * @since 19.03.2004
 * @see ClassUtils
 * @see CollectionUtils
 * @see StringUtils
 */
public abstract class ObjectUtils {

	private static final int INITIAL_HASH = 7;
	private static final int MULTIPLIER = 31;

	private static final String EMPTY_STRING = "";
	private static final String NULL_STRING = "null";
	private static final String ARRAY_START = "{";
	private static final String ARRAY_END = "}";
	private static final String EMPTY_ARRAY = ARRAY_START + ARRAY_END;
	private static final String ARRAY_ELEMENT_SEPARATOR = ", ";
	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
	private static final String NON_EMPTY_ARRAY = ARRAY_START + "..." + ARRAY_END;
	private static final String COLLECTION = "[...]";
	private static final String MAP = NON_EMPTY_ARRAY;


	/**
	 * Return whether the given throwable is a checked exception:
	 * that is, neither a RuntimeException nor an Error.
	 * @param ex the throwable to check
	 * @return whether the throwable is a checked exception
	 * @see java.lang.Exception
	 * @see java.lang.RuntimeException
	 * @see java.lang.Error
	 */
	public static boolean isCheckedException(Throwable ex) {
		return !(ex instanceof RuntimeException || ex instanceof Error);
	}

	/**
	 * Check whether the given exception is compatible with the specified
	 * exception types, as declared in a throws clause.
	 * @param ex the exception to check
	 * @param declaredExceptions the exception types declared in the throws clause
	 * @return whether the given exception is compatible
	 */
	public static boolean isCompatibleWithThrowsClause(Throwable ex, @Nullable Class<?>... declaredExceptions) {
		if (!isCheckedException(ex)) {
			return true;
		}
		if (declaredExceptions != null) {
			for (Class<?> declaredException : declaredExceptions) {
				if (declaredException.isInstance(ex)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determine whether the given object is an array:
	 * either an Object array or a primitive array.
	 * @param obj the object to check
	 */
	public static boolean isArray(@Nullable Object obj) {
		return (obj != null && obj.getClass().isArray());
	}

	/**
	 * Determine whether the given array is empty:
	 * i.e. {@code null} or of zero length.
	 * @param array the array to check
	 * @see #isEmpty(Object)
	 */
	public static boolean isEmpty(@Nullable Object[] array) {
		return (array == null || array.length == 0);
	}

	/**
	 * Determine whether the given object is empty.
	 * <p>This method supports the following object types.
	 * <ul>
	 * <li>{@code Optional}: considered empty if not {@link Optional#isPresent()}</li>
	 * <li>{@code Array}: considered empty if its length is zero</li>
	 * <li>{@link CharSequence}: considered empty if its length is zero</li>
	 * <li>{@link Collection}: delegates to {@link Collection#isEmpty()}</li>
	 * <li>{@link Map}: delegates to {@link Map#isEmpty()}</li>
	 * </ul>
	 * <p>If the given object is non-null and not one of the aforementioned
	 * supported types, this method returns {@code false}.
	 * @param obj the object to check
	 * @return {@code true} if the object is {@code null} or <em>empty</em>
	 * @since 4.2
	 * @see Optional#isPresent()
	 * @see ObjectUtils#isEmpty(Object[])
	 * @see StringUtils#hasLength(CharSequence)
	 * @see CollectionUtils#isEmpty(java.util.Collection)
	 * @see CollectionUtils#isEmpty(java.util.Map)
	 */
	public static boolean isEmpty(@Nullable Object obj) {
		if (obj == null) {
			return true;
		}

		if (obj instanceof Optional) {
			return !((Optional<?>) obj).isPresent();
		}
		if (obj instanceof CharSequence) {
			return ((CharSequence) obj).length() == 0;
		}
		if (obj.getClass().isArray()) {
			return Array.getLength(obj) == 0;
		}
		if (obj instanceof Collection) {
			return ((Collection<?>) obj).isEmpty();
		}
		if (obj instanceof Map) {
			return ((Map<?, ?>) obj).isEmpty();
		}

		// else
		return false;
	}

	/**
	 * Unwrap the given object which is potentially a {@link java.util.Optional}.
	 * @param obj the candidate object
	 * @return either the value held within the {@code Optional}, {@code null}
	 * if the {@code Optional} is empty, or simply the given object as-is
	 * @since 5.0
	 */
	@Nullable
	public static Object unwrapOptional(@Nullable Object obj) {
		if (obj instanceof Optional) {
			Optional<?> optional = (Optional<?>) obj;
			if (!optional.isPresent()) {
				return null;
			}
			Object result = optional.get();
			Assert.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
			return result;
		}
		return obj;
	}

	/**
	 * Check whether the given array contains the given element.
	 * @param array the array to check (may be {@code null},
	 * in which case the return value will always be {@code false})
	 * @param element the element to check for
	 * @return whether the element has been found in the given array
	 */
	public static boolean containsElement(@Nullable Object[] array, Object element) {
		if (array == null) {
			return false;
		}
		for (Object arrayEle : array) {
			if (nullSafeEquals(arrayEle, element)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether the given array of enum constants contains a constant with the given name,
	 * ignoring case when determining a match.
	 * @param enumValues the enum values to check, typically obtained via {@code MyEnum.values()}
	 * @param constant the constant name to find (must not be null or empty string)
	 * @return whether the constant has been found in the given array
	 */
	public static boolean containsConstant(Enum<?>[] enumValues, String constant) {
		return containsConstant(enumValues, constant, false);
	}

	/**
	 * Check whether the given array of enum constants contains a constant with the given name.
	 * @param enumValues the enum values to check, typically obtained via {@code MyEnum.values()}
	 * @param constant the constant name to find (must not be null or empty string)
	 * @param caseSensitive whether case is significant in determining a match
	 * @return whether the constant has been found in the given array
	 */
	public static boolean containsConstant(Enum<?>[] enumValues, String constant, boolean caseSensitive) {
		for (Enum<?> candidate : enumValues) {
			if (caseSensitive ? candidate.toString().equals(constant) :
					candidate.toString().equalsIgnoreCase(constant)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Case insensitive alternative to {@link Enum#valueOf(Class, String)}.
	 * @param <E> the concrete Enum type
	 * @param enumValues the array of all Enum constants in question, usually per {@code Enum.values()}
	 * @param constant the constant to get the enum value of
	 * @throws IllegalArgumentException if the given constant is not found in the given array
	 * of enum values. Use {@link #containsConstant(Enum[], String)} as a guard to avoid this exception.
	 */
	public static <E extends Enum<?>> E caseInsensitiveValueOf(E[] enumValues, String constant) {
		for (E candidate : enumValues) {
			if (candidate.toString().equalsIgnoreCase(constant)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("Constant [" + constant + "] does not exist in enum type " +
				enumValues.getClass().getComponentType().getName());
	}

	/**
	 * Append the given object to the given array, returning a new array
	 * consisting of the input array contents plus the given object.
	 * @param array the array to append to (can be {@code null})
	 * @param obj the object to append
	 * @return the new array (of the same component type; never {@code null})
	 */
	public static <A, O extends A> A[] addObjectToArray(@Nullable A[] array, @Nullable O obj) {
		Class<?> compType = Object.class;
		if (array != null) {
			compType = array.getClass().getComponentType();
		}
		else if (obj != null) {
			compType = obj.getClass();
		}
		int newArrLength = (array != null ? array.length + 1 : 1);
		@SuppressWarnings("unchecked")
		A[] newArr = (A[]) Array.newInstance(compType, newArrLength);
		if (array != null) {
			System.arraycopy(array, 0, newArr, 0, array.length);
		}
		newArr[newArr.length - 1] = obj;
		return newArr;
	}

	/**
	 * Convert the given array (which may be a primitive array) to an
	 * object array (if necessary of primitive wrapper objects).
	 * <p>A {@code null} source value will be converted to an
	 * empty Object array.
	 * @param source the (potentially primitive) array
	 * @return the corresponding object array (never {@code null})
	 * @throws IllegalArgumentException if the parameter is not an array
	 */
	public static Object[] toObjectArray(@Nullable Object source) {
		if (source instanceof Object[]) {
			return (Object[]) source;
		}
		if (source == null) {
			return EMPTY_OBJECT_ARRAY;
		}
		if (!source.getClass().isArray()) {
			throw new IllegalArgumentException("Source is not an array: " + source);
		}
		int length = Array.getLength(source);
		if (length == 0) {
			return EMPTY_OBJECT_ARRAY;
		}
		Class<?> wrapperType = Array.get(source, 0).getClass();
		Object[] newArray = (Object[]) Array.newInstance(wrapperType, length);
		for (int i = 0; i < length; i++) {
			newArray[i] = Array.get(source, i);
		}
		return newArray;
	}


	//---------------------------------------------------------------------
	// Convenience methods for content-based equality/hash-code handling
	//---------------------------------------------------------------------

	/**
	 * Determine if the given objects are equal, returning {@code true} if
	 * both are {@code null} or {@code false} if only one is {@code null}.
	 * <p>Compares arrays with {@code Arrays.equals}, performing an equality
	 * check based on the array elements rather than the array reference.
	 * @param o1 first Object to compare
	 * @param o2 second Object to compare
	 * @return whether the given objects are equal
	 * @see Object#equals(Object)
	 * @see java.util.Arrays#equals
	 */
	public static boolean nullSafeEquals(@Nullable Object o1, @Nullable Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1.equals(o2)) {
			return true;
		}
		if (o1.getClass().isArray() && o2.getClass().isArray()) {
			return arrayEquals(o1, o2);
		}
		return false;
	}

	/**
	 * Compare the given arrays with {@code Arrays.equals}, performing an equality
	 * check based on the array elements rather than the array reference.
	 * @param o1 first array to compare
	 * @param o2 second array to compare
	 * @return whether the given objects are equal
	 * @see #nullSafeEquals(Object, Object)
	 * @see java.util.Arrays#equals
	 */
	private static boolean arrayEquals(Object o1, Object o2) {
		if (o1 instanceof Object[] && o2 instanceof Object[]) {
			return Arrays.equals((Object[]) o1, (Object[]) o2);
		}
		if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
			return Arrays.equals((boolean[]) o1, (boolean[]) o2);
		}
		if (o1 instanceof byte[] && o2 instanceof byte[]) {
			return Arrays.equals((byte[]) o1, (byte[]) o2);
		}
		if (o1 instanceof char[] && o2 instanceof char[]) {
			return Arrays.equals((char[]) o1, (char[]) o2);
		}
		if (o1 instanceof double[] && o2 instanceof double[]) {
			return Arrays.equals((double[]) o1, (double[]) o2);
		}
		if (o1 instanceof float[] && o2 instanceof float[]) {
			return Arrays.equals((float[]) o1, (float[]) o2);
		}
		if (o1 instanceof int[] && o2 instanceof int[]) {
			return Arrays.equals((int[]) o1, (int[]) o2);
		}
		if (o1 instanceof long[] && o2 instanceof long[]) {
			return Arrays.equals((long[]) o1, (long[]) o2);
		}
		if (o1 instanceof short[] && o2 instanceof short[]) {
			return Arrays.equals((short[]) o1, (short[]) o2);
		}
		return false;
	}

	/**
	 * Return as hash code for the given object; typically the value of
	 * {@code Object#hashCode()}}. If the object is an array,
	 * this method will delegate to any of the {@code nullSafeHashCode}
	 * methods for arrays in this class. If the object is {@code null},
	 * this method returns 0.
	 * @see Object#hashCode()
	 * @see #nullSafeHashCode(Object[])
	 * @see #nullSafeHashCode(boolean[])
	 * @see #nullSafeHashCode(byte[])
	 * @see #nullSafeHashCode(char[])
	 * @see #nullSafeHashCode(double[])
	 * @see #nullSafeHashCode(float[])
	 * @see #nullSafeHashCode(int[])
	 * @see #nullSafeHashCode(long[])
	 * @see #nullSafeHashCode(short[])
	 */
	public static int nullSafeHashCode(@Nullable Object obj) {
		if (obj == null) {
			return 0;
		}
		if (obj.getClass().isArray()) {
			if (obj instanceof Object[]) {
				return nullSafeHashCode((Object[]) obj);
			}
			if (obj instanceof boolean[]) {
				return nullSafeHashCode((boolean[]) obj);
			}
			if (obj instanceof byte[]) {
				return nullSafeHashCode((byte[]) obj);
			}
			if (obj instanceof char[]) {
				return nullSafeHashCode((char[]) obj);
			}
			if (obj instanceof double[]) {
				return nullSafeHashCode((double[]) obj);
			}
			if (obj instanceof float[]) {
				return nullSafeHashCode((float[]) obj);
			}
			if (obj instanceof int[]) {
				return nullSafeHashCode((int[]) obj);
			}
			if (obj instanceof long[]) {
				return nullSafeHashCode((long[]) obj);
			}
			if (obj instanceof short[]) {
				return nullSafeHashCode((short[]) obj);
			}
		}
		return obj.hashCode();
	}

	/**
	 * Return a hash code based on the contents of the specified array.
	 * If {@code array} is {@code null}, this method returns 0.
	 */
	public static int nullSafeHashCode(@Nullable Object[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (Object element : array) {
			hash = MULTIPLIER * hash + nullSafeHashCode(element);
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array.
	 * If {@code array} is {@code null}, this method returns 0.
	 */
	public static int nullSafeHashCode(@Nullable boolean[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (boolean element : array) {
			hash = MULTIPLIER * hash + Boolean.hashCode(element);
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array.
	 * If {@code array} is {@code null}, this method returns 0.
	 */
	public static int nullSafeHashCode(@Nullable byte[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (byte element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array.
	 * If {@code array} is {@code null}, this method returns 0.
	 */
	public static int nullSafeHashCode(@Nullable char[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (char element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array.
	 * If {@code array} is {@code null}, this method returns 0.
	 */
	public static int nullSafeHashCode(@Nullable double[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (double element : array) {
			hash = MULTIPLIER * hash + Double.hashCode(element);
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array.
	 * If {@code array} is {@code null}, this method returns 0.
	 */
	public static int nullSafeHashCode(@Nullable float[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (float element : array) {
			hash = MULTIPLIER * hash + Float.hashCode(element);
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array.
	 * If {@code array} is {@code null}, this method returns 0.
	 */
	public static int nullSafeHashCode(@Nullable int[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (int element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array.
	 * If {@code array} is {@code null}, this method returns 0.
	 */
	public static int nullSafeHashCode(@Nullable long[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (long element : array) {
			hash = MULTIPLIER * hash + Long.hashCode(element);
		}
		return hash;
	}

	/**
	 * Return a hash code based on the contents of the specified array.
	 * If {@code array} is {@code null}, this method returns 0.
	 */
	public static int nullSafeHashCode(@Nullable short[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (short element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * Return the same value as {@link Boolean#hashCode(boolean)}}.
	 * @deprecated as of Spring Framework 5.0, in favor of the native JDK 8 variant
	 */
	@Deprecated
	public static int hashCode(boolean bool) {
		return Boolean.hashCode(bool);
	}

	/**
	 * Return the same value as {@link Double#hashCode(double)}}.
	 * @deprecated as of Spring Framework 5.0, in favor of the native JDK 8 variant
	 */
	@Deprecated
	public static int hashCode(double dbl) {
		return Double.hashCode(dbl);
	}

	/**
	 * Return the same value as {@link Float#hashCode(float)}}.
	 * @deprecated as of Spring Framework 5.0, in favor of the native JDK 8 variant
	 */
	@Deprecated
	public static int hashCode(float flt) {
		return Float.hashCode(flt);
	}

	/**
	 * Return the same value as {@link Long#hashCode(long)}}.
	 * @deprecated as of Spring Framework 5.0, in favor of the native JDK 8 variant
	 */
	@Deprecated
	public static int hashCode(long lng) {
		return Long.hashCode(lng);
	}


	//---------------------------------------------------------------------
	// Convenience methods for toString output
	//---------------------------------------------------------------------

	/**
	 * Return a String representation of an object's overall identity.
	 * @param obj the object (may be {@code null})
	 * @return the object's identity as String representation,
	 * or an empty String if the object was {@code null}
	 */
	public static String identityToString(@Nullable Object obj) {
		if (obj == null) {
			return EMPTY_STRING;
		}
		return obj.getClass().getName() + "@" + getIdentityHexString(obj);
	}

	/**
	 * Return a hex String form of an object's identity hash code.
	 * @param obj the object
	 * @return the object's identity code in hex notation
	 */
	public static String getIdentityHexString(Object obj) {
		return Integer.toHexString(System.identityHashCode(obj));
	}

	/**
	 * Return a content-based String representation if {@code obj} is
	 * not {@code null}; otherwise returns an empty String.
	 * <p>Differs from {@link #nullSafeToString(Object)} in that it returns
	 * an empty String rather than "null" for a {@code null} value.
	 * @param obj the object to build a display String for
	 * @return a display String representation of {@code obj}
	 * @see #nullSafeToString(Object)
	 */
	public static String getDisplayString(@Nullable Object obj) {
		if (obj == null) {
			return EMPTY_STRING;
		}
		return nullSafeToString(obj);
	}

	/**
	 * Determine the class name for the given object.
	 * <p>Returns a {@code "null"} String if {@code obj} is {@code null}.
	 * @param obj the object to introspect (may be {@code null})
	 * @return the corresponding class name
	 */
	public static String nullSafeClassName(@Nullable Object obj) {
		return (obj != null ? obj.getClass().getName() : NULL_STRING);
	}

	/**
	 * Return a String representation of the specified Object.
	 * <p>Builds a String representation of the contents in case of an array.
	 * Returns a {@code "null"} String if {@code obj} is {@code null}.
	 * @param obj the object to build a String representation for
	 * @return a String representation of {@code obj}
	 * @see #nullSafeConciseToString(Object)
	 */
	public static String nullSafeToString(@Nullable Object obj) {
		if (obj == null) {
			return NULL_STRING;
		}
		if (obj instanceof String) {
			return (String) obj;
		}
		if (obj instanceof Object[]) {
			return nullSafeToString((Object[]) obj);
		}
		if (obj instanceof boolean[]) {
			return nullSafeToString((boolean[]) obj);
		}
		if (obj instanceof byte[]) {
			return nullSafeToString((byte[]) obj);
		}
		if (obj instanceof char[]) {
			return nullSafeToString((char[]) obj);
		}
		if (obj instanceof double[]) {
			return nullSafeToString((double[]) obj);
		}
		if (obj instanceof float[]) {
			return nullSafeToString((float[]) obj);
		}
		if (obj instanceof int[]) {
			return nullSafeToString((int[]) obj);
		}
		if (obj instanceof long[]) {
			return nullSafeToString((long[]) obj);
		}
		if (obj instanceof short[]) {
			return nullSafeToString((short[]) obj);
		}
		String str = obj.toString();
		return (str != null ? str : EMPTY_STRING);
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(@Nullable Object[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (Object o : array) {
			stringJoiner.add(String.valueOf(o));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(@Nullable boolean[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (boolean b : array) {
			stringJoiner.add(String.valueOf(b));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(@Nullable byte[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (byte b : array) {
			stringJoiner.add(String.valueOf(b));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(@Nullable char[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (char c : array) {
			stringJoiner.add('\'' + String.valueOf(c) + '\'');
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(@Nullable double[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (double d : array) {
			stringJoiner.add(String.valueOf(d));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(@Nullable float[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (float f : array) {
			stringJoiner.add(String.valueOf(f));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(@Nullable int[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (int i : array) {
			stringJoiner.add(String.valueOf(i));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(@Nullable long[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (long l : array) {
			stringJoiner.add(String.valueOf(l));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(@Nullable short[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (short s : array) {
			stringJoiner.add(String.valueOf(s));
		}
		return stringJoiner.toString();
	}

	/**
	 * Generate a null-safe, concise string representation of the supplied object
	 * as described below.
	 * <p>Favor this method over {@link #nullSafeToString(Object)} when you need
	 * the length of the generated string to be limited.
	 * <p>Returns:
	 * <ul>
	 * <li>{@code "null"} if {@code obj} is {@code null}</li>
	 * <li>{@code"Optional.empty"} if {@code obj} is an empty {@link Optional}</li>
	 * <li>{@code"Optional[<concise-string>]"} if {@code obj} is a non-empty {@code Optional},
	 * where {@code <concise-string>} is the result of invoking {@link #nullSafeConciseToString}
	 * on the object contained in the {@code Optional}</li>
	 * <li>{@code "{}"} if {@code obj} is an empty array</li>
	 * <li>{@code "{...}"} if {@code obj} is a {@link Map} or a non-empty array</li>
	 * <li>{@code "[...]"} if {@code obj} is a {@link Collection}</li>
	 * <li>{@linkplain Class#getName() Class name} if {@code obj} is a {@link Class}</li>
	 * <li>{@linkplain Charset#name() Charset name} if {@code obj} is a {@link Charset}</li>
	 * <li>{@linkplain TimeZone#getID() TimeZone ID} if {@code obj} is a {@link TimeZone}</li>
	 * <li>{@linkplain ZoneId#getId() Zone ID} if {@code obj} is a {@link ZoneId}</li>
	 * <li>Potentially {@linkplain StringUtils#truncate(CharSequence) truncated string}
	 * if {@code obj} is a {@link String} or {@link CharSequence}</li>
	 * <li>Potentially {@linkplain StringUtils#truncate(CharSequence) truncated string}
	 * if {@code obj} is a <em>simple value type</em> whose {@code toString()} method
	 * returns a non-null value</li>
	 * <li>Otherwise, a string representation of the object's type name concatenated
	 * with {@code "@"} and a hex string form of the object's identity hash code</li>
	 * </ul>
	 * <p>In the context of this method, a <em>simple value type</em> is any of the following:
	 * primitive wrapper (excluding {@link Void}), {@link Enum}, {@link Number},
	 * {@link Date}, {@link Temporal}, {@link File}, {@link Path}, {@link URI},
	 * {@link URL}, {@link InetAddress}, {@link Currency}, {@link Locale},
	 * {@link UUID}, {@link Pattern}.
	 * @param obj the object to build a string representation for
	 * @return a concise string representation of the supplied object
	 * @since 5.3.27
	 * @see #nullSafeToString(Object)
	 * @see StringUtils#truncate(CharSequence)
	 */
	public static String nullSafeConciseToString(@Nullable Object obj) {
		if (obj == null) {
			return "null";
		}
		if (obj instanceof Optional<?>) {
			Optional<?> optional = (Optional<?>) obj;
			return (!optional.isPresent() ? "Optional.empty" :
					String.format("Optional[%s]", nullSafeConciseToString(optional.get())));
		}
		if (obj.getClass().isArray()) {
			return (Array.getLength(obj) == 0 ? EMPTY_ARRAY : NON_EMPTY_ARRAY);
		}
		if (obj instanceof Collection) {
			return COLLECTION;
		}
		if (obj instanceof Map) {
			return MAP;
		}
		if (obj instanceof Class<?>) {
			return ((Class<?>) obj).getName();
		}
		if (obj instanceof Charset) {
			return ((Charset) obj).name();
		}
		if (obj instanceof TimeZone) {
			return ((TimeZone) obj).getID();
		}
		if (obj instanceof ZoneId) {
			return ((ZoneId) obj).getId();
		}
		if (obj instanceof CharSequence) {
			return StringUtils.truncate((CharSequence) obj);
		}
		Class<?> type = obj.getClass();
		if (isSimpleValueType(type)) {
			String str = obj.toString();
			if (str != null) {
				return StringUtils.truncate(str);
			}
		}
		return type.getTypeName() + "@" + getIdentityHexString(obj);
	}

	/**
	 * Derived from {@link org.springframework.beans.BeanUtils#isSimpleValueType}.
	 * <p>As of 5.3.28, considering {@link UUID} in addition to the bean-level check.
	 * <p>As of 5.3.29, additionally considering {@link File}, {@link Path},
	 * {@link InetAddress}, {@link Charset}, {@link Currency}, {@link TimeZone},
	 * {@link ZoneId}, {@link Pattern}.
	 */
	private static boolean isSimpleValueType(Class<?> type) {
		return (Void.class != type && void.class != type &&
				(ClassUtils.isPrimitiveOrWrapper(type) ||
				Enum.class.isAssignableFrom(type) ||
				CharSequence.class.isAssignableFrom(type) ||
				Number.class.isAssignableFrom(type) ||
				Date.class.isAssignableFrom(type) ||
				Temporal.class.isAssignableFrom(type) ||
				ZoneId.class.isAssignableFrom(type) ||
				TimeZone.class.isAssignableFrom(type) ||
				File.class.isAssignableFrom(type) ||
				Path.class.isAssignableFrom(type) ||
				Charset.class.isAssignableFrom(type) ||
				Currency.class.isAssignableFrom(type) ||
				InetAddress.class.isAssignableFrom(type) ||
				URI.class == type ||
				URL.class == type ||
				UUID.class == type ||
				Locale.class == type ||
				Pattern.class == type ||
				Class.class == type));
	}

}
