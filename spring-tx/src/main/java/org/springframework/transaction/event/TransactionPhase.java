/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.event;

import java.util.function.Consumer;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * The phase in which a transactional event listener applies.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.2
 * @see TransactionalEventListener#phase()
 * @see TransactionalApplicationListener#getTransactionPhase()
 * @see TransactionalApplicationListener#forPayload(TransactionPhase, Consumer)
 */
public enum TransactionPhase {

	/**
	 * Handle the event before transaction commit.
	 * @see TransactionSynchronization#beforeCommit(boolean)
	 */
	BEFORE_COMMIT,

	/**
	 * Handle the event after the commit has completed successfully.
	 * <p>Note: This is a specialization of {@link #AFTER_COMPLETION} and therefore
	 * executes in the same sequence of events as {@code AFTER_COMPLETION}
	 * (and not in {@link TransactionSynchronization#afterCommit()}).
	 * <p>Interactions with the underlying transactional resource will not be
	 * committed in this phase. See
	 * {@link TransactionSynchronization#afterCompletion(int)} for details.
	 * @see TransactionSynchronization#afterCompletion(int)
	 * @see TransactionSynchronization#STATUS_COMMITTED
	 */
	AFTER_COMMIT,

	/**
	 * Handle the event if the transaction has rolled back.
	 * <p>Note: This is a specialization of {@link #AFTER_COMPLETION} and therefore
	 * executes in the same sequence of events as {@code AFTER_COMPLETION}.
	 * <p>Interactions with the underlying transactional resource will not be
	 * committed in this phase. See
	 * {@link TransactionSynchronization#afterCompletion(int)} for details.
	 * @see TransactionSynchronization#afterCompletion(int)
	 * @see TransactionSynchronization#STATUS_ROLLED_BACK
	 */
	AFTER_ROLLBACK,

	/**
	 * Handle the event after the transaction has completed.
	 * <p>For more fine-grained events, use {@link #AFTER_COMMIT} or
	 * {@link #AFTER_ROLLBACK} to intercept transaction commit
	 * or rollback, respectively.
	 * <p>Interactions with the underlying transactional resource will not be
	 * committed in this phase. See
	 * {@link TransactionSynchronization#afterCompletion(int)} for details.
	 * @see TransactionSynchronization#afterCompletion(int)
	 */
	AFTER_COMPLETION

}
