/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.transaction.event;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * The phase at which a transactional event listener applies.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.2
 * @see TransactionalEventListener
 */
public enum TransactionPhase {

	/**
	 * Fire the event before transaction commit.
	 * @see TransactionSynchronization#beforeCommit(boolean)
	 */
	BEFORE_COMMIT,

	/**
	 * Fire the event after the commit has completed successfully.
	 * <p>Note: This is a specialization of {@link #AFTER_COMPLETION} and
	 * therefore executes in the same after-completion sequence of events,
	 * (and not in {@link TransactionSynchronization#afterCommit()}).
	 * @see TransactionSynchronization#afterCompletion(int)
	 * @see TransactionSynchronization#STATUS_COMMITTED
	 */
	AFTER_COMMIT,

	/**
	 * Fire the event if the transaction has rolled back.
	 * <p>Note: This is a specialization of {@link #AFTER_COMPLETION} and
	 * therefore executes in the same after-completion sequence of events.
	 * @see TransactionSynchronization#afterCompletion(int)
	 * @see TransactionSynchronization#STATUS_ROLLED_BACK
	 */
	AFTER_ROLLBACK,

	/**
	 * Fire the event after the transaction has completed.
	 * <p>For more fine-grained events, use {@link #AFTER_COMMIT} or
	 * {@link #AFTER_ROLLBACK} to intercept transaction commit
	 * or rollback, respectively.
	 * @see TransactionSynchronization#afterCompletion(int)
	 */
	AFTER_COMPLETION

}
