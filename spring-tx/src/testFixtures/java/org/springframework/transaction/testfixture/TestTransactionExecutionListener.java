/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.transaction.testfixture;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;

/**
 * @author Juergen Hoeller
 * @since 6.1
 */
public class TestTransactionExecutionListener implements TransactionExecutionListener {

	public boolean beforeBeginCalled;

	public boolean afterBeginCalled;

	@Nullable
	public Throwable beginFailure;

	public boolean beforeCommitCalled;

	public boolean afterCommitCalled;

	@Nullable
	public Throwable commitFailure;

	public boolean beforeRollbackCalled;

	public boolean afterRollbackCalled;

	@Nullable
	public Throwable rollbackFailure;


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
