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
