/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.testfixture;

import org.jspecify.annotations.Nullable;

import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;

/**
 * @author Juergen Hoeller
 * @since 6.1
 */
public class TestTransactionExecutionListener implements TransactionExecutionListener {

	public boolean beforeBeginCalled;

	public boolean afterBeginCalled;

	public @Nullable Throwable beginFailure;

	public boolean beforeCommitCalled;

	public boolean afterCommitCalled;

	public @Nullable Throwable commitFailure;

	public boolean beforeRollbackCalled;

	public boolean afterRollbackCalled;

	public @Nullable Throwable rollbackFailure;


	@Override
	public void beforeBegin(TransactionExecution transaction) {
		this.beforeBeginCalled = true;
	}

	@Override
	public void afterBegin(TransactionExecution transaction, @Nullable Throwable beginFailure) {
		this.afterBeginCalled = true;
		this.beginFailure = beginFailure;
	}

	@Override
	public void beforeCommit(TransactionExecution transaction) {
		this.beforeCommitCalled = true;
	}

	@Override
	public void afterCommit(TransactionExecution transaction, @Nullable Throwable commitFailure) {
		this.afterCommitCalled = true;
		this.commitFailure = commitFailure;
	}

	@Override
	public void beforeRollback(TransactionExecution transaction) {
		this.beforeRollbackCalled = true;
	}

	@Override
	public void afterRollback(TransactionExecution transaction, @Nullable Throwable rollbackFailure) {
		this.afterRollbackCalled = true;
		this.rollbackFailure = rollbackFailure;
	}

}
