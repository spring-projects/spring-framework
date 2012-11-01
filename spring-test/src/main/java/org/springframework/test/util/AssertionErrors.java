/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.test.util;

/**
 * JUnit independent assertion class.
 *
 * @author Lukas Krecan
 * @author Arjen Poutsma
 * @since 3.2
 */
public abstract class AssertionErrors {

    private AssertionErrors() {
    }

	/**
	 * Fails a test with the given message.
	 *
	 * @param message the message
	 */
    public static void fail(String message) {
        throw new AssertionError(message);
    }

	/**
	 * Fails a test with the given message passing along expected and actual
	 * values to be added to the message.
	 *
	 * @param message the message
	 * @param expected the expected value
	 * @param actual the actual value
	 */
    public static void fail(String message, Object expected, Object actual) {
        throw new AssertionError(message + " expected:<" + expected + "> but was:<" + actual + ">");
    }

	/**
	 * Asserts that a condition is {@code true}. If not, throws an
	 * {@link AssertionError} with the given message.
	 *
	 * @param message the message
	 * @param condition the condition to test for
	 */
    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            fail(message);
        }
    }

	/**
	 * Asserts that two objects are equal. If not, an {@link AssertionError} is
	 * thrown with the given message.
	 *
	 * @param message the message
	 * @param expected the expected value
	 * @param actual the actual value
	 */
    public static void assertEquals(String message, Object expected, Object actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }
        fail(message, expected, actual);
    }

}
