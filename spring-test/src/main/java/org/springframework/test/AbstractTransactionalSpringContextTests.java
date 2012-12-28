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

package org.springframework.test;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Convenient base class for JUnit 3.8 based tests that should occur in a
 * transaction, but normally will roll the transaction back on the completion of
 * each test.
 *
 * <p>This is useful in a range of circumstances, allowing the following benefits:
 * <ul>
 * <li>Ability to delete or insert any data in the database, without affecting
 * other tests
 * <li>Providing a transactional context for any code requiring a transaction
 * <li>Ability to write anything to the database without any need to clean up.
 * </ul>
 *
 * <p>This class is typically very fast, compared to traditional setup/teardown
 * scripts.
 *
 * <p>If data should be left in the database, call the {@link #setComplete()}
 * method in each test. The {@link #setDefaultRollback "defaultRollback"}
 * property, which defaults to "true", determines whether transactions will
 * complete by default.
 *
 * <p>It is even possible to end the transaction early; for example, to verify lazy
 * loading behavior of an O/R mapping tool. (This is a valuable away to avoid
 * unexpected errors when testing a web UI, for example.) Simply call the
 * {@link #endTransaction()} method. Execution will then occur without a
 * transactional context.
 *
 * <p>The {@link #startNewTransaction()} method may be called after a call to
 * {@link #endTransaction()} if you wish to create a new transaction, quite
 * independent of the old transaction. The new transaction's default fate will
 * be to roll back, unless {@link #setComplete()} is called again during the
 * scope of the new transaction. Any number of transactions may be created and
 * ended in this way. The final transaction will automatically be rolled back
 * when the test case is torn down.
 *
 * <p>Transactional behavior requires a single bean in the context implementing the
 * {@link PlatformTransactionManager} interface. This will be set by the
 * superclass's Dependency Injection mechanism. If using the superclass's Field
 * Injection mechanism, the implementation should be named "transactionManager".
 * This mechanism allows the use of the
 * {@link AbstractDependencyInjectionSpringContextTests} superclass even when
 * there is more than one transaction manager in the context.
 *
 * <p><b>This base class can also be used without transaction management, if no
 * PlatformTransactionManager bean is found in the context provided.</b> Be
 * careful about using this mode, as it allows the potential to permanently
 * modify data. This mode is available only if dependency checking is turned off
 * in the {@link AbstractDependencyInjectionSpringContextTests} superclass. The
 * non-transactional capability is provided to enable use of the same subclass
 * in different environments.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.1.1
 * @deprecated as of Spring 3.0, in favor of using the listener-based test context framework
 * ({@link org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests})
 */
@Deprecated
public abstract class AbstractTransactionalSpringContextTests extends AbstractDependencyInjectionSpringContextTests {

	/** The transaction manager to use */
	protected PlatformTransactionManager transactionManager;

	/** Should we roll back by default? */
	private boolean defaultRollback = true;

	/** Should we commit the current transaction? */
	private boolean complete = false;

	/** Number of transactions started */
	private int transactionsStarted = 0;

	/**
	 * Transaction definition used by this test class: by default, a plain
	 * DefaultTransactionDefinition. Subclasses can change this to cause
	 * different behavior.
	 */
	protected TransactionDefinition	transactionDefinition= new DefaultTransactionDefinition();

	/**
	 * TransactionStatus for this test. Typical subclasses won't need to use it.
	 */
	protected TransactionStatus	transactionStatus;


	/**
	 * Default constructor for AbstractTransactionalSpringContextTests.
	 */
	public AbstractTransactionalSpringContextTests() {
	}

	/**
	 * Constructor for AbstractTransactionalSpringContextTests with a JUnit name.
	 */
	public AbstractTransactionalSpringContextTests(String name) {
		super(name);
	}


	/**
	 * Specify the transaction manager to use. No transaction management will be
	 * available if this is not set. Populated through dependency injection by
	 * the superclass.
	 * <p>
	 * This mode works only if dependency checking is turned off in the
	 * {@link AbstractDependencyInjectionSpringContextTests} superclass.
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Subclasses can set this value in their constructor to change the default,
	 * which is always to roll the transaction back.
	 */
	public void setDefaultRollback(final boolean defaultRollback) {
		this.defaultRollback = defaultRollback;
	}
	/**
	 * Get the <em>default rollback</em> flag for this test.
	 * @see #setDefaultRollback(boolean)
	 * @return The <em>default rollback</em> flag.
	 */
	protected boolean isDefaultRollback() {
		return this.defaultRollback;
	}

	/**
	 * Determines whether or not to rollback transactions for the current test.
	 * <p>The default implementation delegates to {@link #isDefaultRollback()}.
	 * Subclasses can override as necessary.
	 */
	protected boolean isRollback() {
		return isDefaultRollback();
	}

	/**
	 * Call this method in an overridden {@link #runBare()} method to prevent
	 * transactional execution.
	 */
	protected void preventTransaction() {
		this.transactionDefinition = null;
	}

	/**
	 * Call this method in an overridden {@link #runBare()} method to override
	 * the transaction attributes that will be used, so that {@link #setUp()}
	 * and {@link #tearDown()} behavior is modified.
	 * @param customDefinition the custom transaction definition
	 */
	protected void setTransactionDefinition(TransactionDefinition customDefinition) {
		this.transactionDefinition = customDefinition;
	}

	/**
	 * This implementation creates a transaction before test execution.
	 * <p>Override {@link #onSetUpBeforeTransaction()} and/or
	 * {@link #onSetUpInTransaction()} to add custom set-up behavior for
	 * transactional execution. Alternatively, override this method for general
	 * set-up behavior, calling {@code super.onSetUp()} as part of your
	 * method implementation.
	 * @throws Exception simply let any exception propagate
	 * @see #onTearDown()
	 */
	@Override
	protected void onSetUp() throws Exception {
		this.complete = !this.isRollback();

		if (this.transactionManager == null) {
			this.logger.info("No transaction manager set: test will NOT run within a transaction");
		}
		else if (this.transactionDefinition == null) {
			this.logger.info("No transaction definition set: test will NOT run within a transaction");
		}
		else {
			onSetUpBeforeTransaction();
			startNewTransaction();
			try {
				onSetUpInTransaction();
			}
			catch (final Exception ex) {
				endTransaction();
				throw ex;
			}
		}
	}

	/**
	 * Subclasses can override this method to perform any setup operations, such
	 * as populating a database table, <i>before</i> the transaction created by
	 * this class. Only invoked if there <i>is</i> a transaction: that is, if
	 * {@link #preventTransaction()} has not been invoked in an overridden
	 * {@link #runTest()} method.
	 * @throws Exception simply let any exception propagate
	 */
	protected void onSetUpBeforeTransaction() throws Exception {
	}

	/**
	 * Subclasses can override this method to perform any setup operations, such
	 * as populating a database table, <i>within</i> the transaction created by
	 * this class.
	 * <p><b>NB:</b> Not called if there is no transaction management, due to no
	 * transaction manager being provided in the context.
	 * <p>If any {@link Throwable} is thrown, the transaction that has been started
	 * prior to the execution of this method will be
	 * {@link #endTransaction() ended} (or rather an attempt will be made to
	 * {@link #endTransaction() end it gracefully}); The offending
	 * {@link Throwable} will then be rethrown.
	 * @throws Exception simply let any exception propagate
	 */
	protected void onSetUpInTransaction() throws Exception {
	}

	/**
	 * This implementation ends the transaction after test execution.
	 * <p>Override {@link #onTearDownInTransaction()} and/or
	 * {@link #onTearDownAfterTransaction()} to add custom tear-down behavior
	 * for transactional execution. Alternatively, override this method for
	 * general tear-down behavior, calling {@code super.onTearDown()} as
	 * part of your method implementation.
	 * <p>Note that {@link #onTearDownInTransaction()} will only be called if a
	 * transaction is still active at the time of the test shutdown. In
	 * particular, it will <i>not</i> be called if the transaction has been
	 * completed with an explicit {@link #endTransaction()} call before.
	 * @throws Exception simply let any exception propagate
	 * @see #onSetUp()
	 */
	@Override
	protected void onTearDown() throws Exception {
		// Call onTearDownInTransaction and end transaction if the transaction
		// is still active.
		if (this.transactionStatus != null && !this.transactionStatus.isCompleted()) {
			try {
				onTearDownInTransaction();
			}
			finally {
				endTransaction();
			}
		}

		// Call onTearDownAfterTransaction if there was at least one
		// transaction, even if it has been completed early through an
		// endTransaction() call.
		if (this.transactionsStarted > 0) {
			onTearDownAfterTransaction();
		}
	}

	/**
	 * Subclasses can override this method to run invariant tests here. The
	 * transaction is <i>still active</i> at this point, so any changes made in
	 * the transaction will still be visible. However, there is no need to clean
	 * up the database, as a rollback will follow automatically.
	 * <p><b>NB:</b> Not called if there is no actual transaction, for example due
	 * to no transaction manager being provided in the application context.
	 * @throws Exception simply let any exception propagate
	 */
	protected void onTearDownInTransaction() throws Exception {
	}

	/**
	 * Subclasses can override this method to perform cleanup after a
	 * transaction here. At this point, the transaction is <i>not active anymore</i>.
	 * @throws Exception simply let any exception propagate
	 */
	protected void onTearDownAfterTransaction() throws Exception {
	}

	/**
	 * Cause the transaction to commit for this test method, even if the test
	 * method is configured to {@link #isRollback() rollback}.
	 * @throws IllegalStateException if the operation cannot be set to complete
	 * as no transaction manager was provided
	 */
	protected void setComplete() {
		if (this.transactionManager == null) {
			throw new IllegalStateException("No transaction manager set");
		}
		this.complete = true;
	}

	/**
	 * Immediately force a commit or rollback of the transaction, according to
	 * the {@code complete} and {@link #isRollback() rollback} flags.
	 * <p>Can be used to explicitly let the transaction end early, for example to
	 * check whether lazy associations of persistent objects work outside of a
	 * transaction (that is, have been initialized properly).
	 * @see #setComplete()
	 */
	protected void endTransaction() {
		final boolean commit = this.complete || !isRollback();
		if (this.transactionStatus != null) {
			try {
				if (commit) {
					this.transactionManager.commit(this.transactionStatus);
					this.logger.debug("Committed transaction after execution of test [" + getName() + "].");
				}
				else {
					this.transactionManager.rollback(this.transactionStatus);
					this.logger.debug("Rolled back transaction after execution of test [" + getName() + "].");
				}
			}
			finally {
				this.transactionStatus = null;
			}
		}
	}

	/**
	 * Start a new transaction. Only call this method if
	 * {@link #endTransaction()} has been called. {@link #setComplete()} can be
	 * used again in the new transaction. The fate of the new transaction, by
	 * default, will be the usual rollback.
	 * @throws TransactionException if starting the transaction failed
	 */
	protected void startNewTransaction() throws TransactionException {
		if (this.transactionStatus != null) {
			throw new IllegalStateException("Cannot start new transaction without ending existing transaction: "
					+ "Invoke endTransaction() before startNewTransaction()");
		}
		if (this.transactionManager == null) {
			throw new IllegalStateException("No transaction manager set");
		}

		this.transactionStatus = this.transactionManager.getTransaction(this.transactionDefinition);
		++this.transactionsStarted;
		this.complete = !this.isRollback();

		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Began transaction (" + this.transactionsStarted + "): transaction manager ["
					+ this.transactionManager + "]; rollback [" + this.isRollback() + "].");
		}
	}

}
