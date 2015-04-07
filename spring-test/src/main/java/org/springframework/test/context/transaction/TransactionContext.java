/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * Transaction context for a specific {@link TestContext}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 */
class TransactionContext {

	private static final Log logger = LogFactory.getLog(TransactionContext.class);

	private final TestContext testContext;

	private final TransactionDefinition transactionDefinition;

	private final PlatformTransactionManager transactionManager;

	private final boolean defaultRollback;

	private boolean flaggedForRollback;

	private TransactionStatus transactionStatus;

	private volatile int transactionsStarted = 0;


	TransactionContext(TestContext testContext, PlatformTransactionManager transactionManager,
			TransactionDefinition transactionDefinition, boolean defaultRollback) {
		this.testContext = testContext;
		this.transactionManager = transactionManager;
		this.transactionDefinition = transactionDefinition;
		this.defaultRollback = defaultRollback;
		this.flaggedForRollback = defaultRollback;
	}

	TransactionStatus getTransactionStatus() {
		return this.transactionStatus;
	}

	/**
	 * Has the current transaction been flagged for rollback?
	 * <p>In other words, should we roll back or commit the current transaction
	 * upon completion of the current test?
	 */
	boolean isFlaggedForRollback() {
		return this.flaggedForRollback;
	}

	void setFlaggedForRollback(boolean flaggedForRollback) {
		if (this.transactionStatus == null) {
			throw new IllegalStateException(String.format(
				"Failed to set rollback flag for test context %s: transaction does not exist.", this.testContext));
		}
		this.flaggedForRollback = flaggedForRollback;
	}

	/**
	 * Start a new transaction for the configured {@linkplain #getTestContext test context}.
	 * <p>Only call this method if {@link #endTransaction} has been called or if no
	 * transaction has been previously started.
	 * @throws TransactionException if starting the transaction fails
	 */
	void startTransaction() {
		if (this.transactionStatus != null) {
			throw new IllegalStateException(
				"Cannot start a new transaction without ending the existing transaction first.");
		}
		this.flaggedForRollback = this.defaultRollback;
		this.transactionStatus = this.transactionManager.getTransaction(this.transactionDefinition);
		++this.transactionsStarted;
		if (logger.isInfoEnabled()) {
			logger.info(String.format(
				"Began transaction (%s) for test context %s; transaction manager [%s]; rollback [%s]",
				this.transactionsStarted, this.testContext, this.transactionManager, flaggedForRollback));
		}
	}

	/**
	 * Immediately force a <em>commit</em> or <em>rollback</em> of the transaction
	 * for the configured {@linkplain #getTestContext test context}, according to
	 * the {@linkplain #isFlaggedForRollback rollback flag}.
	 */
	void endTransaction() {
		if (logger.isTraceEnabled()) {
			logger.trace(String.format(
				"Ending transaction for test context %s; transaction status [%s]; rollback [%s]", this.testContext,
				this.transactionStatus, flaggedForRollback));
		}
		if (this.transactionStatus == null) {
			throw new IllegalStateException(String.format(
				"Failed to end transaction for test context %s: transaction does not exist.", this.testContext));
		}

		try {
			if (flaggedForRollback) {
				this.transactionManager.rollback(this.transactionStatus);
			}
			else {
				this.transactionManager.commit(this.transactionStatus);
			}
		}
		finally {
			this.transactionStatus = null;
		}

		if (logger.isInfoEnabled()) {
			logger.info(String.format("%s transaction for test context %s.", (flaggedForRollback ? "Rolled back"
					: "Committed"), this.testContext));
		}
	}

}