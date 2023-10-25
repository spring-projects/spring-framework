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

package org.springframework.transaction.reactive;

import org.springframework.lang.Nullable;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link ReactiveTransaction} interface,
 * used by {@link AbstractReactiveTransactionManager}. Based on the concept
 * of an underlying "transaction object".
 *
 * <p>Holds all status information that {@link AbstractReactiveTransactionManager}
 * needs internally, including a generic transaction object determined by the
 * concrete transaction manager implementation.
 *
 * <p><b>NOTE:</b> This is <i>not</i> intended for use with other ReactiveTransactionManager
 * implementations, in particular not for mock transaction managers in testing environments.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see AbstractReactiveTransactionManager
 * @see #getTransaction
 */
public class GenericReactiveTransaction implements ReactiveTransaction {

	@Nullable
	private final String transactionName;

	@Nullable
	private final Object transaction;

	private final boolean newTransaction;

	private final boolean newSynchronization;

	private final boolean nested;

	private final boolean readOnly;

	private final boolean debug;

	@Nullable
	private final Object suspendedResources;

	private boolean rollbackOnly = false;

	private boolean completed = false;


	/**
	 * Create a new {@code DefaultReactiveTransactionStatus} instance.
	 * @param transactionName the defined name of the transaction
	 * @param transaction underlying transaction object that can hold state
	 * for the internal transaction implementation
	 * @param newTransaction if the transaction is new, otherwise participating
	 * in an existing transaction
	 * @param newSynchronization if a new transaction synchronization has been
	 * opened for the given transaction
	 * @param readOnly whether the transaction is marked as read-only
	 * @param debug should debug logging be enabled for the handling of this transaction?
	 * Caching it in here can prevent repeated calls to ask the logging system whether
	 * debug logging should be enabled.
	 * @param suspendedResources a holder for resources that have been suspended
	 * for this transaction, if any
	 * @since 6.1
	 */
	public GenericReactiveTransaction(
			@Nullable String transactionName, @Nullable Object transaction, boolean newTransaction,
			boolean newSynchronization, boolean nested, boolean readOnly, boolean debug,
			@Nullable Object suspendedResources) {

		this.transactionName = transactionName;
		this.transaction = transaction;
		this.newTransaction = newTransaction;
		this.newSynchronization = newSynchronization;
		this.nested = nested;
		this.readOnly = readOnly;
		this.debug = debug;
		this.suspendedResources = suspendedResources;
	}

	@Deprecated(since = "6.1", forRemoval = true)
	public GenericReactiveTransaction(@Nullable Object transaction, boolean newTransaction,
			boolean newSynchronization, boolean readOnly, boolean debug, @Nullable Object suspendedResources) {

		this(null, transaction, newTransaction, newSynchronization, false, readOnly, debug, suspendedResources);
	}


	@Override
	public String getTransactionName() {
		return (this.transactionName != null ? this.transactionName : "");
	}

	/**
	 * Return the underlying transaction object.
	 * @throws IllegalStateException if no transaction is active
	 */
	public Object getTransaction() {
		Assert.state(this.transaction != null, "No transaction active");
		return this.transaction;
	}

	@Override
	public boolean hasTransaction() {
		return (this.transaction != null);
	}

	@Override
	public boolean isNewTransaction() {
		return (hasTransaction() && this.newTransaction);
	}

	/**
	 * Return if a new transaction synchronization has been opened for this transaction.
	 */
	public boolean isNewSynchronization() {
		return this.newSynchronization;
	}

	@Override
	public boolean isNested() {
		return this.nested;
	}

	@Override
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * Return whether the progress of this transaction is debugged. This is used by
	 * {@link AbstractReactiveTransactionManager} as an optimization, to prevent repeated
	 * calls to {@code logger.isDebugEnabled()}. Not really intended for client code.
	 */
	public boolean isDebug() {
		return this.debug;
	}

	/**
	 * Return the holder for resources that have been suspended for this transaction,
	 * if any.
	 */
	@Nullable
	public Object getSuspendedResources() {
		return this.suspendedResources;
	}

	@Override
	public void setRollbackOnly() {
		if (this.completed) {
			throw new IllegalStateException("Transaction completed");
		}
		this.rollbackOnly = true;
	}

	/**
	 * Determine the rollback-only flag via checking this ReactiveTransactionStatus.
	 * <p>Will only return "true" if the application called {@code setRollbackOnly}
	 * on this TransactionStatus object.
	 */
	@Override
	public boolean isRollbackOnly() {
		return this.rollbackOnly;
	}

	/**
	 * Mark this transaction as completed, that is, committed or rolled back.
	 */
	public void setCompleted() {
		this.completed = true;
	}

	@Override
	public boolean isCompleted() {
		return this.completed;
	}

}
