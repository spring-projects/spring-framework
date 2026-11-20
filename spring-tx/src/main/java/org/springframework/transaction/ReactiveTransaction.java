/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

/**
 * Representation of an ongoing {@link ReactiveTransactionManager} transaction.
 * This is currently a marker interface extending {@link TransactionExecution}
 * but may acquire further methods in a future revision.
 *
 * <p>Transactional code can use this to retrieve status information,
 * and to programmatically request a rollback (instead of throwing
 * an exception that causes an implicit rollback).
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see #setRollbackOnly()
 * @see ReactiveTransactionManager#getReactiveTransaction
 * @see org.springframework.transaction.reactive.TransactionCallback#doInTransaction
 */
public interface ReactiveTransaction extends TransactionExecution {

}
