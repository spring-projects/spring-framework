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

/**
 * Common representation of the current state of a transaction.
 * Serves as base interface for {@link TransactionStatus} as well as
 * {@link ReactiveTransaction}, and as of 6.1 also as transaction
 * representation for {@link TransactionExecutionListener}.
 *
 * @author Juergen Hoeller
 * @since 5.2
 */
public interface TransactionExecution {

	/**
	 * Return the defined name of the transaction (possibly an empty String).
	 * <p>In case of Spring's declarative transactions, the exposed name will be
	 * the {@code fully-qualified class name + "." + method name} (by default).
	 * <p>The default implementation returns an empty String.
	 * @since 6.1
	 * @see TransactionDefinition#getName()
	 */
	default String getTransactionName() {
		return "";
	}

	/**
	 * Return whether there is an actual transaction active: this is meant to cover
	 * a new transaction as well as participation in an existing transaction, only
	 * returning {@code false} when not running in an actual transaction at all.
	 * <p>The default implementation returns {@code true}.
	 * @since 6.1
	 * @see #isNewTransaction()
	 * @see #isNested()
	 * @see #isReadOnly()
	 */
	default boolean hasTransaction() {
		return true;
	}

	/**
	 * Return whether the transaction manager considers the present transaction
	 * as new; otherwise participating in an existing transaction, or potentially
	 * not running in an actual transaction in the first place.
	 * <p>This is primarily here for transaction manager state handling.
	 * Prefer the use of {@link #hasTransaction()} for application purposes
	 * since this is usually semantically appropriate.
	 * <p>The "new" status can be transaction manager specific, e.g. returning
	 * {@code true} for an actual nested transaction but potentially {@code false}
	 * for a savepoint-based nested transaction scope if the savepoint management
	 * is explicitly exposed (such as on {@link TransactionStatus}). A combined
	 * check for any kind of nested execution is provided by {@link #isNested()}.
	 * <p>The default implementation returns {@code true}.
	 * @see #hasTransaction()
	 * @see #isNested()
	 * @see TransactionStatus#hasSavepoint()
	 */
	default boolean isNewTransaction() {
		return true;
	}

	/**
	 * Return if this transaction executes in a nested fashion within another.
	 * <p>The default implementation returns {@code false}.
	 * @since 6.1
	 * @see #hasTransaction()
	 * @see #isNewTransaction()
	 * @see TransactionDefinition#PROPAGATION_NESTED
	 */
	default boolean isNested() {
		return false;
	}

	/**
	 * Return if this transaction is defined as read-only transaction.
	 * <p>The default implementation returns {@code false}.
	 * @since 6.1
	 * @see TransactionDefinition#isReadOnly()
	 */
	default boolean isReadOnly() {
		return false;
	}

	/**
	 * Set the transaction rollback-only. This instructs the transaction manager
	 * that the only possible outcome of the transaction may be a rollback, as
	 * alternative to throwing an exception which would in turn trigger a rollback.
	 * <p>The default implementation throws an UnsupportedOperationException.
	 * @see #isRollbackOnly()
	 */
	default void setRollbackOnly() {
		throw new UnsupportedOperationException("setRollbackOnly not supported");
	}

	/**
	 * Return whether the transaction has been marked as rollback-only
	 * (either by the application or by the transaction infrastructure).
	 * <p>The default implementation returns {@code false}.
	 * @see #setRollbackOnly()
	 */
	default boolean isRollbackOnly() {
		return false;
	}

	/**
	 * Return whether this transaction is completed, that is,
	 * whether it has already been committed or rolled back.
	 * <p>The default implementation returns {@code false}.
	 */
	default boolean isCompleted() {
		return false;
	}

}
