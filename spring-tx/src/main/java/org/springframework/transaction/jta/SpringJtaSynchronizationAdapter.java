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

package org.springframework.transaction.jta;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Adapter that implements the JTA {@link javax.transaction.Synchronization}
 * interface delegating to an underlying Spring
 * {@link org.springframework.transaction.support.TransactionSynchronization}.
 *
 * <p>Useful for synchronizing Spring resource management code with plain
 * JTA / EJB CMT transactions, despite the original code being built for
 * Spring transaction synchronization.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see javax.transaction.Transaction#registerSynchronization
 * @see org.springframework.transaction.support.TransactionSynchronization
 */
public class SpringJtaSynchronizationAdapter implements Synchronization {

	protected static final Log logger = LogFactory.getLog(SpringJtaSynchronizationAdapter.class);

	private final TransactionSynchronization springSynchronization;

	private UserTransaction jtaTransaction;

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
	 * <p>Note that this adapter will never perform a rollback-only call on WebLogic,
	 * since WebLogic Server is known to automatically mark the transaction as
	 * rollback-only in case of a <code>beforeCompletion</code> exception. Hence,
	 * on WLS, this constructor is equivalent to the single-arg constructor.
	 * @param springSynchronization the Spring TransactionSynchronization to delegate to
	 * @param jtaUserTransaction the JTA UserTransaction to use for rollback-only
	 * setting in case of an exception thrown in <code>beforeCompletion</code>
	 * (can be omitted if the JTA provider itself marks the transaction rollback-only
	 * in such a scenario, which is required by the JTA specification as of JTA 1.1).
	 */
	public SpringJtaSynchronizationAdapter(
			TransactionSynchronization springSynchronization, UserTransaction jtaUserTransaction) {

		this(springSynchronization);
		if (jtaUserTransaction != null && !jtaUserTransaction.getClass().getName().startsWith("weblogic.")) {
			this.jtaTransaction = jtaUserTransaction;
		}
	}

	/**
	 * Create a new SpringJtaSynchronizationAdapter for the given Spring
	 * TransactionSynchronization and JTA TransactionManager.
	 * <p>Note that this adapter will never perform a rollback-only call on WebLogic,
	 * since WebLogic Server is known to automatically mark the transaction as
	 * rollback-only in case of a <code>beforeCompletion</code> exception. Hence,
	 * on WLS, this constructor is equivalent to the single-arg constructor.
	 * @param springSynchronization the Spring TransactionSynchronization to delegate to
	 * @param jtaTransactionManager the JTA TransactionManager to use for rollback-only
	 * setting in case of an exception thrown in <code>beforeCompletion</code>
	 * (can be omitted if the JTA provider itself marks the transaction rollback-only
	 * in such a scenario, which is required by the JTA specification as of JTA 1.1)
	 */
	public SpringJtaSynchronizationAdapter(
			TransactionSynchronization springSynchronization, TransactionManager jtaTransactionManager) {

		this(springSynchronization);
		if (jtaTransactionManager != null && !jtaTransactionManager.getClass().getName().startsWith("weblogic.")) {
			this.jtaTransaction = new UserTransactionAdapter(jtaTransactionManager);
		}
	}


	/**
	 * JTA <code>beforeCompletion</code> callback: just invoked before commit.
	 * <p>In case of an exception, the JTA transaction will be marked as rollback-only.
	 * @see org.springframework.transaction.support.TransactionSynchronization#beforeCommit
	 */
	public void beforeCompletion() {
		try {
			boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
			this.springSynchronization.beforeCommit(readOnly);
		}
		catch (RuntimeException ex) {
			setRollbackOnlyIfPossible();
			throw ex;
		}
		catch (Error err) {
			setRollbackOnlyIfPossible();
			throw err;
		}
		finally {
			// Process Spring's beforeCompletion early, in order to avoid issues
			// with strict JTA implementations that issue warnings when doing JDBC
			// operations after transaction completion (e.g. Connection.getWarnings).
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
	 * JTA <code>afterCompletion</code> callback: invoked after commit/rollback.
	 * <p>Needs to invoke the Spring synchronization's <code>beforeCompletion</code>
	 * at this late stage in case of a rollback, since there is no corresponding
	 * callback with JTA.
	 * @see org.springframework.transaction.support.TransactionSynchronization#beforeCompletion
	 * @see org.springframework.transaction.support.TransactionSynchronization#afterCompletion
	 */
	public void afterCompletion(int status) {
		if (!this.beforeCompletionCalled) {
			// beforeCompletion not called before (probably because of JTA rollback).
			// Perform the cleanup here.
			this.springSynchronization.beforeCompletion();
		}
		// Call afterCompletion with the appropriate status indication.
		switch (status) {
			case Status.STATUS_COMMITTED:
				this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
				break;
			case Status.STATUS_ROLLEDBACK:
				this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
				break;
			default:
				this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
		}
	}

}
