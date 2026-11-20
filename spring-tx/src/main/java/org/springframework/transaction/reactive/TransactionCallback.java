/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.reactive;

import org.reactivestreams.Publisher;

import org.springframework.transaction.ReactiveTransaction;

/**
 * Callback interface for reactive transactional code. Used with {@link TransactionalOperator}'s
 * {@code execute} method, often as anonymous class within a method implementation.
 *
 * <p>Typically used to assemble various calls to transaction-unaware data access
 * services into a higher-level service method with transaction demarcation. As an
 * alternative, consider the use of declarative transaction demarcation (for example, through
 * Spring's {@link org.springframework.transaction.annotation.Transactional} annotation).
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @param <T> the result type
 * @see TransactionalOperator
 */
@FunctionalInterface
public interface TransactionCallback<T> {

	/**
	 * Gets called by {@link TransactionalOperator} within a transactional context.
	 * Does not need to care about transactions itself, although it can retrieve and
	 * influence the status of the current transaction via the given status object,
	 * for example, setting rollback-only.
	 * @param status associated transaction status
	 * @return a result publisher
	 * @see TransactionalOperator#transactional
	 */
	Publisher<T> doInTransaction(ReactiveTransaction status);

}
