/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.transaction.ReactiveTransactionStatus;

/**
 * Abstract base implementation of the {@link ReactiveTransactionStatus} interface.
 *
 * <p>Pre-implements the handling of local rollback-only and completed flags.
 *
 * <p>Does not assume any specific internal transaction handling, such as an
 * underlying transaction object, and no transaction synchronization mechanism.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see #setRollbackOnly()
 * @see #isRollbackOnly()
 * @see #setCompleted()
 * @see #isCompleted()
 * @see DefaultReactiveTransactionStatus
 */
public abstract class AbstractReactiveTransactionStatus implements ReactiveTransactionStatus {

	private boolean rollbackOnly = false;

	private boolean completed = false;


	//---------------------------------------------------------------------
	// Handling of current transaction state
	//---------------------------------------------------------------------

	@Override
	public void setRollbackOnly() {
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
