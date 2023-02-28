/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.transaction;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

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

	@Nullable
	private TransactionStatus transactionStatus;

	private final AtomicInteger transactionsStarted = new AtomicInteger();


	TransactionContext(TestContext testContext, PlatformTransactionManager transactionManager,
			TransactionDefinition transactionDefinition, boolean defaultRollback) {

		this.testContext = testContext;
		this.transactionManager = transactionManager;
		this.transactionDefinition = transactionDefinition;
		this.defaultRollback = defaultRollback;
		this.flaggedForRollback = defaultRollback;
	}


	@Nullable
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
		Assert.state(this.transactionStatus != null, () ->
				"Failed to set rollback flag - transaction does not exist: " + this.testContext);
		this.flaggedForRollback = flaggedForRollback;
	}

	/**
	 * Start a new transaction for the configured test context.
	 * <p>Only call this method if {@link #endTransaction} has been called or if no
	 * transaction has been previously started.
	 * @throws TransactionException if starting the transaction fails
	 */
	void startTransaction() {
		Assert.state(this.transactionStatus == null,
				"Cannot start a new transaction without ending the existing transaction first");

		this.flaggedForRollback = this.defaultRollback;
		this.transactionStatus = this.transactionManager.getTransaction(this.transactionDefinition);
		int transactionsStarted = this.transactionsStarted.incrementAndGet();

		if (logger.isTraceEnabled()) {
			logger.trace("Began transaction (%d) for test context %s; transaction manager [%s]; rollback [%s]"
					.formatted(transactionsStarted, this.testContext, this.transactionManager, this.flaggedForRollback));
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Began transaction (%d) for test class [%s]; test method [%s]; transaction manager [%s]; rollback [%s]"
					.formatted(transactionsStarted, this.testContext.getTestClass().getName(),
						this.testContext.getTestMethod().getName(), this.transactionManager, this.flaggedForRollback));
		}
	}

	/**
	 * Immediately force a <em>commit</em> or <em>rollback</em> of the transaction for the
	 * configured test context, according to the {@linkplain #isFlaggedForRollback rollback flag}.
	 */
	void endTransaction() {
		if (logger.isTraceEnabled()) {
			logger.trace(String.format(
					"Ending transaction for test context %s; transaction status [%s]; rollback [%s]",
					this.testContext, this.transactionStatus, this.flaggedForRollback));
		}
		Assert.state(this.transactionStatus != null,
				() -> "Failed to end transaction - transaction does not exist: " + this.testContext);

		try {
			if (this.flaggedForRollback) {
				this.transactionManager.rollback(this.transactionStatus);
			}
			else {
				this.transactionManager.commit(this.transactionStatus);
			}
		}
		finally {
			this.transactionStatus = null;
		}

		int transactionsStarted = this.transactionsStarted.get();
		if (logger.isTraceEnabled()) {
			logger.trace("%s transaction (%d) for test context: %s"
					.formatted((this.flaggedForRollback ? "Rolled back" : "Committed"),
						transactionsStarted, this.testContext));
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("%s transaction (%d) for test class [%s]; test method [%s]"
					.formatted((this.flaggedForRollback ? "Rolled back" : "Committed"), transactionsStarted,
						this.testContext.getTestClass().getName(), this.testContext.getTestMethod().getName()));
		}
	}

}
