/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.transaction.jta;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Adapter that implements the JTA {@link jakarta.transaction.Synchronization}
 * interface delegating to an underlying Spring
 * {@link org.springframework.transaction.support.TransactionSynchronization}.
 *
 * <p>Useful for synchronizing Spring resource management code with plain
 * JTA / EJB CMT transactions, despite the original code being built for
 * Spring transaction synchronization.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see jakarta.transaction.Transaction#registerSynchronization
 * @see org.springframework.transaction.support.TransactionSynchronization
 */
public class SpringJtaSynchronizationAdapter implements Synchronization {

	protected static final Log logger = LogFactory.getLog(SpringJtaSynchronizationAdapter.class);

	private final TransactionSynchronization springSynchronization;

	private @Nullable UserTransaction jtaTransaction;

	private boolean beforeCompletionCalled = false;


	/**
	 * Create a new SpringJtaSynchronizationAdapter for the given Spring
	 * TransactionSynchronization and JTA TransactionManager.
	 * @param springSynchronization the Spring TransactionSynchronization to delegate to
	 */
	public SpringJtaSynchronizationAdapter(TransactionSynchronization springSynchronization) {
		Assert.notNull(springSynchronization, "TransactionSynchronization must not be null");
		this.springSynchronization = springSynchronization;
	}

	/**
	 * Create a new SpringJtaSynchronizationAdapter for the given Spring
	 * TransactionSynchronization and JTA TransactionManager.
	 * @param springSynchronization the Spring TransactionSynchronization to delegate to
	 * @param jtaUserTransaction the JTA UserTransaction to use for rollback-only
	 * setting in case of an exception thrown in {@code beforeCompletion}
	 * @deprecated as of 6.0.12 since JTA 1.1+ requires implicit rollback-only setting
	 * in case of an exception thrown in {@code beforeCompletion}, so the regular
	 * {@link #SpringJtaSynchronizationAdapter(TransactionSynchronization)} constructor
	 * is sufficient for all scenarios
	 */
	@Deprecated(since = "6.0.12")
	public SpringJtaSynchronizationAdapter(TransactionSynchronization springSynchronization,
			@Nullable UserTransaction jtaUserTransaction) {

		this(springSynchronization);
		this.jtaTransaction = jtaUserTransaction;
	}

	/**
	 * Create a new SpringJtaSynchronizationAdapter for the given Spring
	 * TransactionSynchronization and JTA TransactionManager.
	 * @param springSynchronization the Spring TransactionSynchronization to delegate to
	 * @param jtaTransactionManager the JTA TransactionManager to use for rollback-only
	 * setting in case of an exception thrown in {@code beforeCompletion}
	 * @deprecated as of 6.0.12 since JTA 1.1+ requires implicit rollback-only setting
	 * in case of an exception thrown in {@code beforeCompletion}, so the regular
	 * {@link #SpringJtaSynchronizationAdapter(TransactionSynchronization)} constructor
	 * is sufficient for all scenarios
	 */
	@Deprecated(since = "6.0.12")
	public SpringJtaSynchronizationAdapter(TransactionSynchronization springSynchronization,
			@Nullable TransactionManager jtaTransactionManager) {

		this(springSynchronization);
		this.jtaTransaction =
				(jtaTransactionManager != null ? new UserTransactionAdapter(jtaTransactionManager) : null);
	}


	/**
	 * JTA {@code beforeCompletion} callback: just invoked before commit.
	 * <p>In case of an exception, the JTA transaction will be marked as rollback-only.
	 * @see org.springframework.transaction.support.TransactionSynchronization#beforeCommit
	 */
	@Override
	public void beforeCompletion() {
		try {
			boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
			this.springSynchronization.beforeCommit(readOnly);
		}
		catch (RuntimeException | Error ex) {
			setRollbackOnlyIfPossible();
			throw ex;
		}
		finally {
			// Process Spring's beforeCompletion early, in order to avoid issues
			// with strict JTA implementations that issue warnings when doing JDBC
			// operations after transaction completion (for example, Connection.getWarnings).
			this.beforeCompletionCalled = true;
			this.springSynchronization.beforeCompletion();
		}
	}

	/**
	 * Set the underlying JTA transaction to rollback-only.
	 */
	private void setRollbackOnlyIfPossible() {
		if (this.jtaTransaction != null) {
			try {
				this.jtaTransaction.setRollbackOnly();
			}
			catch (UnsupportedOperationException ex) {
				// Probably Hibernate's WebSphereExtendedJTATransactionLookup pseudo JTA stuff...
				logger.debug("JTA transaction handle does not support setRollbackOnly method - " +
						"relying on JTA provider to mark the transaction as rollback-only based on " +
						"the exception thrown from beforeCompletion", ex);
			}
			catch (Throwable ex) {
				logger.error("Could not set JTA transaction rollback-only", ex);
			}
		}
		else {
			logger.debug("No JTA transaction handle available and/or running on WebLogic - " +
						"relying on JTA provider to mark the transaction as rollback-only based on " +
						"the exception thrown from beforeCompletion");
			}
	}

	/**
	 * JTA {@code afterCompletion} callback: invoked after commit/rollback.
	 * <p>Needs to invoke the Spring synchronization's {@code beforeCompletion}
	 * at this late stage in case of a rollback, since there is no corresponding
	 * callback with JTA.
	 * @see org.springframework.transaction.support.TransactionSynchronization#beforeCompletion
	 * @see org.springframework.transaction.support.TransactionSynchronization#afterCompletion
	 */
	@Override
	public void afterCompletion(int status) {
		if (!this.beforeCompletionCalled) {
			// beforeCompletion not called before (probably because of JTA rollback).
			// Perform the cleanup here.
			this.springSynchronization.beforeCompletion();
		}
		// Call afterCompletion with the appropriate status indication.
		switch (status) {
			case Status.STATUS_COMMITTED ->
				this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
			case Status.STATUS_ROLLEDBACK ->
				this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
			default ->
				this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
		}
	}

}
