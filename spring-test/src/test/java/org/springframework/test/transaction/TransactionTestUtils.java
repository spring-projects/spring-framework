/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.transaction;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Collection of JDK 1.4+ utilities for tests involving transactions. Intended
 * for internal use within the Spring testing suite.
 *
 * <p>All {@code assert*()} methods throw {@link AssertionError}s.
 *
 * @author Sam Brannen
 * @since 2.5
 */
public abstract class TransactionTestUtils {

	/**
	 * Convenience method for determining if a transaction is active for the
	 * current {@link Thread}.
	 * @return {@code true} if a transaction is currently active
	 */
	public static boolean inTransaction() {
		return TransactionSynchronizationManager.isActualTransactionActive();
	}

	/**
	 * Asserts whether or not a transaction is active for the current
	 * {@link Thread}.
	 * @param transactionExpected whether or not a transaction is expected
	 * @throws AssertionError if the supplied assertion fails
	 * @see #inTransaction()
	 */
	public static void assertInTransaction(boolean transactionExpected) {
		if (transactionExpected) {
			assertCondition(inTransaction(), "The current thread should be associated with a transaction.");
		}
		else {
			assertCondition(!inTransaction(), "The current thread should not be associated with a transaction");
		}
	}

	/**
	 * Fails by throwing an {@code AssertionError} with the supplied
	 * {@code message}.
	 * @param message the exception message to use
	 * @see #assertCondition(boolean, String)
	 */
	private static void fail(String message) throws AssertionError {
		throw new AssertionError(message);
	}

	/**
	 * Assert the provided boolean {@code condition}, throwing
	 * {@code AssertionError} with the supplied {@code message} if
	 * the test result is {@code false}.
	 * @param condition a boolean expression
	 * @param message the exception message to use if the assertion fails
	 * @throws AssertionError if condition is {@code false}
	 * @see #fail(String)
	 */
	private static void assertCondition(boolean condition, String message) throws AssertionError {
		if (!condition) {
			fail(message);
		}
	}

}
