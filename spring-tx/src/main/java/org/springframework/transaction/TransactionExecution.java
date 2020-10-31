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

package org.springframework.transaction;

/**
 * Common representation of the current state of a transaction.
 * Serves as base interface for {@link TransactionStatus} as well as
 * {@link ReactiveTransaction}.
 *
 * @author Juergen Hoeller
 * @since 5.2
 */
public interface TransactionExecution {

	/**
	 * Return whether the present transaction is new; otherwise participating
	 * in an existing transaction, or potentially not running in an actual
	 * transaction in the first place.
	 */
	boolean isNewTransaction();

	/**
	 * Set the transaction rollback-only. This instructs the transaction manager
	 * that the only possible outcome of the transaction may be a rollback, as
	 * alternative to throwing an exception which would in turn trigger a rollback.
	 */
	void setRollbackOnly();

	/**
	 * Return whether the transaction has been marked as rollback-only
	 * (either by the application or by the transaction infrastructure).
	 */
	boolean isRollbackOnly();

	/**
	 * Return whether this transaction is completed, that is,
	 * whether it has already been committed or rolled back.
	 */
	boolean isCompleted();

}
