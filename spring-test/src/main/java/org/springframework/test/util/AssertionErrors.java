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

package org.springframework.test.util;

import org.springframework.lang.Contract;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Test assertions that are independent of any third-party assertion library.
 *
 * @author Lukas Krecan
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 3.2
 */
public abstract class AssertionErrors {

	/**
	 * Fail a test with the given message.
	 * @param message a message that describes the reason for the failure
	 */
	@Contract("_ -> fail")
	public static void fail(String message) {
		throw new AssertionError(message);
	}

	/**
	 * Fail a test with the given message passing along expected and actual
	 * values to be appended to the message.
	 * <p>For example given:
	 * <pre class="code">
	 * String name = "Accept";
	 * String expected = "application/json";
	 * String actual = "text/plain";
	 * fail("Response header [" + name + "]", expected, actual);
	 * </pre>
	 * <p>The resulting message is:
	 * <pre class="code">
	 * Response header [Accept] expected:&lt;application/json&gt; but was:&lt;text/plain&gt;
	 * </pre>
	 * @param message a message that describes the use case that failed
	 * @param expected the expected value
	 * @param actual the actual value
	 */
	public static void fail(String message, @Nullable Object expected, @Nullable Object actual) {
		throw new AssertionError(message + " expected:<" + expected + "> but was:<" + actual + ">");
	}

	/**
	 * Assert the given condition is {@code true} and raise an
	 * {@link AssertionError} otherwise.
	 * @param message a message that describes the reason for the failure
	 * @param condition the condition to test for
	 */
	@Contract("_, false -> fail")
	public static void assertTrue(String message, boolean condition) {
		if (!condition) {
			fail(message);
		}
	}

	/**
	 * Assert the given condition is {@code false} and raise an
	 * {@link AssertionError} otherwise.
	 * @param message a message that describes the reason for the failure
	 * @param condition the condition to test for
	 * @since 5.2.1
	 */
	@Contract("_, true -> fail")
	public static void assertFalse(String message, boolean condition) {
		if (condition) {
			fail(message);
		}
	}

	/**
	 * Assert that the given object is {@code null} and raise an
	 * {@link AssertionError} otherwise.
	 * @param message a message that describes the reason for the failure
	 * @param object the object to check
	 * @since 5.2.1
	 */
	@Contract("_, !null -> fail")
	public static void assertNull(String message, @Nullable Object object) {
		assertTrue(message, object == null);
	}

	/**
	 * Assert that the given object is not {@code null} and raise an
	 * {@link AssertionError} otherwise.
	 * @param message a message that describes the reason for the failure
	 * @param object the object to check
	 * @since 5.1.8
	 */
	@Contract("_, null -> fail")
	public static void assertNotNull(String message, @Nullable Object object) {
		assertTrue(message, object != null);
	}

	/**
	 * Assert two objects are equal and raise an {@link AssertionError} otherwise.
	 * <p>For example:
	 * <pre class="code">
	 * assertEquals("Response header [" + name + "]", expected, actual);
	 * </pre>
	 * @param message a message that describes the value being checked
	 * @param expected the expected value
	 * @param actual the actual value
	 * @see #fail(String, Object, Object)
	 */
	public static void assertEquals(String message, @Nullable Object expected, @Nullable Object actual) {
		if (!ObjectUtils.nullSafeEquals(expected, actual)) {
			fail(message, ObjectUtils.nullSafeToString(expected), ObjectUtils.nullSafeToString(actual));
		}
	}

	/**
	 * Assert two objects are not equal and raise an {@link AssertionError} otherwise.
	 * <p>For example:
	 * <pre class="code">
	 * assertNotEquals("Response header [" + name + "]", expected, actual);
	 * </pre>
	 * @param message a message that describes the value being checked
	 * @param expected the expected value
	 * @param actual the actual value
	 */
	public static void assertNotEquals(String message, @Nullable Object expected, @Nullable Object actual) {
		if (ObjectUtils.nullSafeEquals(expected, actual)) {
			throw new AssertionError(message + " was not expected to be:" +
					"<" + ObjectUtils.nullSafeToString(actual) + ">");
		}
	}

}
