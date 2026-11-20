/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.transaction;

import org.assertj.core.api.AbstractBooleanAssert;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Collection of assertions for tests involving transactions.
 *
 * <p>Intended for internal use within the Spring testing suite.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 5.2
 */
public final class TransactionAssert {

	private static final TransactionAssert instance = new TransactionAssert();

	public static TransactionAssert assertThatTransaction() {
		return instance;
	}

	public void isActive() {
		isInTransaction(true);
	}

	public void isNotActive() {
		isInTransaction(false);
	}

	private void isInTransaction(boolean expected) {
		AbstractBooleanAssert<?> assertInTransaction =
				assertThat(TransactionSynchronizationManager.isActualTransactionActive())
					.as("active transaction");
		if (expected) {
			assertInTransaction.isTrue();
		}
		else {
			assertInTransaction.isFalse();
		}
	}

}
