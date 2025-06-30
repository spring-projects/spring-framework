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

package org.springframework.transaction.reactive;

import reactor.core.publisher.Mono;

/**
 * Interface for reactive transaction synchronization callbacks.
 * Supported by {@link AbstractReactiveTransactionManager}.
 *
 * <p>TransactionSynchronization implementations can implement the
 * {@link org.springframework.core.Ordered} interface to influence their execution order.
 * A synchronization that does not implement the {@link org.springframework.core.Ordered}
 * interface is appended to the end of the synchronization chain.
 *
 * <p>System synchronizations performed by Spring itself use specific order values,
 * allowing for fine-grained interaction with their execution order (if necessary).
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see TransactionSynchronizationManager
 * @see AbstractReactiveTransactionManager
 */
public interface TransactionSynchronization {

	/** Completion status in case of proper commit. */
	int STATUS_COMMITTED = 0;

	/** Completion status in case of proper rollback. */
	int STATUS_ROLLED_BACK = 1;

	/** Completion status in case of heuristic mixed completion or system errors. */
	int STATUS_UNKNOWN = 2;


	/**
	 * Suspend this synchronization.
	 * Supposed to unbind resources from TransactionSynchronizationManager if managing any.
	 * @see TransactionSynchronizationManager#unbindResource
	 */
	default Mono<Void> suspend() {
		return Mono.empty();
	}

	/**
	 * Resume this synchronization.
	 * Supposed to rebind resources to TransactionSynchronizationManager if managing any.
	 * @see TransactionSynchronizationManager#bindResource
	 */
	default Mono<Void> resume() {
		return Mono.empty();
	}

	/**
	 * Invoked before transaction commit (before "beforeCompletion").
	 * <p>This callback does <i>not</i> mean that the transaction will actually be committed.
	 * A rollback decision can still occur after this method has been called. This callback
	 * is rather meant to perform work that's only relevant if a commit still has a chance
	 * to happen, such as flushing SQL statements to the database.
	 * <p>Note that exceptions will get propagated to the commit caller and cause a
	 * rollback of the transaction.
	 * @param readOnly whether the transaction is defined as read-only transaction
	 * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
	 * (note: do not throw TransactionException subclasses here!)
	 * @see #beforeCompletion
	 */
	default Mono<Void> beforeCommit(boolean readOnly) {
		return Mono.empty();
	}

	/**
	 * Invoked before transaction commit/rollback.
	 * Can perform resource cleanup <i>before</i> transaction completion.
	 * <p>This method will be invoked after {@code beforeCommit}, even when
	 * {@code beforeCommit} threw an exception. This callback allows for
	 * closing resources before transaction completion, for any outcome.
	 * @throws RuntimeException in case of errors; will be <b>logged but not propagated</b>
	 * (note: do not throw TransactionException subclasses here!)
	 * @see #beforeCommit
	 * @see #afterCompletion
	 */
	default Mono<Void> beforeCompletion() {
		return Mono.empty();
	}

	/**
	 * Invoked after transaction commit. Can perform further operations right
	 * <i>after</i> the main transaction has <i>successfully</i> committed.
	 * <p>Can, for example, commit further operations that are supposed to follow on a successful
	 * commit of the main transaction, like confirmation messages or emails.
	 * <p><b>NOTE:</b> The transaction will have been committed already, but the
	 * transactional resources might still be active and accessible. As a consequence,
	 * any data access code triggered at this point will still "participate" in the
	 * original transaction, allowing to perform some cleanup (with no commit following
	 * anymore!), unless it explicitly declares that it needs to run in a separate
	 * transaction. Hence: <b>Use {@code PROPAGATION_REQUIRES_NEW} for any
	 * transactional operation that is called from here.</b>
	 * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
	 * (note: do not throw TransactionException subclasses here!)
	 */
	default Mono<Void> afterCommit() {
		return Mono.empty();
	}

	/**
	 * Invoked after transaction commit/rollback.
	 * Can perform resource cleanup <i>after</i> transaction completion.
	 * <p><b>NOTE:</b> The transaction will have been committed or rolled back already,
	 * but the transactional resources might still be active and accessible. As a
	 * consequence, any data access code triggered at this point will still "participate"
	 * in the original transaction, allowing to perform some cleanup (with no commit
	 * following anymore!), unless it explicitly declares that it needs to run in a
	 * separate transaction. Hence: <b>Use {@code PROPAGATION_REQUIRES_NEW}
	 * for any transactional operation that is called from here.</b>
	 * @param status completion status according to the {@code STATUS_*} constants
	 * @throws RuntimeException in case of errors; will be <b>logged but not propagated</b>
	 * (note: do not throw TransactionException subclasses here!)
	 * @see #STATUS_COMMITTED
	 * @see #STATUS_ROLLED_BACK
	 * @see #STATUS_UNKNOWN
	 * @see #beforeCompletion
	 */
	default Mono<Void> afterCompletion(int status) {
		return Mono.empty();
	}

}
