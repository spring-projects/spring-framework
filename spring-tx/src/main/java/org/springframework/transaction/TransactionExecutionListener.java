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

package org.springframework.transaction;

import org.springframework.lang.Nullable;

/**
 * Callback interface for stateless listening to transaction creation/completion steps
 * in a transaction manager. This is primarily meant for observation and statistics;
 * consider stateful transaction synchronizations for resource management purposes.
 *
 * <p>In contrast to synchronizations, the transaction execution listener contract is
 * commonly supported for thread-bound transactions as well as reactive transactions.
 * The callback-provided {@link TransactionExecution} object will be either a
 * {@link TransactionStatus} (for a {@link PlatformTransactionManager} transaction) or
 * a {@link ReactiveTransaction} (for a {@link ReactiveTransactionManager} transaction).
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see ConfigurableTransactionManager#addListener
 * @see org.springframework.transaction.support.TransactionSynchronizationManager#registerSynchronization
 * @see org.springframework.transaction.reactive.TransactionSynchronizationManager#registerSynchronization
 */
public interface TransactionExecutionListener {

	/**
	 * Callback before the transaction begin step.
	 * @param transaction the current transaction
	 */
	default void beforeBegin(TransactionExecution transaction) {
	}

	/**
	 * Callback after the transaction begin step.
	 * @param transaction the current transaction
	 * @param beginFailure an exception occurring during begin
	 * (or {@code null} after a successful begin step)
	 */
	default void afterBegin(TransactionExecution transaction, @Nullable Throwable beginFailure) {
	}

	/**
	 * Callback before the transaction commit step.
	 * @param transaction the current transaction
	 */
	default void beforeCommit(TransactionExecution transaction) {
	}

	/**
	 * Callback after the transaction commit step.
	 * @param transaction the current transaction
	 * @param commitFailure an exception occurring during commit
	 * (or {@code null} after a successful commit step)
	 */
	default void afterCommit(TransactionExecution transaction, @Nullable Throwable commitFailure) {
	}

	/**
	 * Callback before the transaction rollback step.
	 * @param transaction the current transaction
	 */
	default void beforeRollback(TransactionExecution transaction) {
	}

	/**
	 * Callback after the transaction rollback step.
	 * @param transaction the current transaction
	 * @param rollbackFailure an exception occurring during rollback
	 * (or {@code null} after a successful rollback step)
	 */
	default void afterRollback(TransactionExecution transaction, @Nullable Throwable rollbackFailure) {
	}

}
