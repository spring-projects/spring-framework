/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * JUnit independent assertion class.
 *
 * @author Lukas Krecan
 * @author Arjen Poutsma
 * @since 3.2
 */
public abstract class AssertionErrors {

	/**
	 * Fails a test with the given message.
	 * @param message describes the reason for the failure
	 */
	public static void fail(String message) {
		throw new AssertionError(message);
	}

	/**
	 * Fails a test with the given message passing along expected and actual
	 * values to be added to the message.
	 * <p>For example given:
	 * <pre class="code">
	 * assertEquals("Response header [" + name + "]", actual, expected);
	 * </pre>
	 * <p>The resulting message is:
	 * <pre class="code">
	 * Response header [Accept] expected:&lt;application/json&gt; but was:&lt;text/plain&gt;
	 * </pre>
	 * @param message describes the value that failed the match
	 * @param expected expected value
	 * @param actual actual value
	 */
	public static void fail(String message, @Nullable Object expected, @Nullable Object actual) {
		throw new AssertionError(message + " expected:<" + expected + "> but was:<" + actual + ">");
	}

	/**
	 * Assert the given condition is {@code true} and raise an
	 * {@link AssertionError} if it is not.
	 * @param message the message
	 * @param condition the condition to test for
	 */
	public static void assertTrue(String message, boolean condition) {
		if (!condition) {
			fail(message);
		}
	}

	/**
	 * Assert two objects are equal and raise an {@link AssertionError} if not.
	 * <p>For example:
	 * <pre class="code">
	 * assertEquals("Response header [" + name + "]", expected, actual);
	 * </pre>
	 * @param message describes the value being checked
	 * @param expected the expected value
	 * @param actual the actual value
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
	 * @param message describes the value being checked
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
