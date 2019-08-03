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

package org.springframework.test.transaction;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Collection of assertions for tests involving transactions. Intended for
 * internal use within the Spring testing suite.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 5.2
 */
public class TransactionAssert {

	private static final TransactionAssert instance = new TransactionAssert();

	public TransactionAssert isActive() {
		return isInTransaction(true);
	}

	public TransactionAssert isNotActive() {
		return isInTransaction(false);

	}

	public TransactionAssert isInTransaction(boolean expected) {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive())
				.as("active transaction")
				.isEqualTo(expected);
		return this;
	}

	public static TransactionAssert assertThatTransaction() {
		return instance;
	}

}
