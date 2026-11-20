/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

import java.util.Collection;

/**
 * Common configuration interface for transaction manager implementations.
 * Provides registration facilities for {@link TransactionExecutionListener}.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see PlatformTransactionManager
 * @see ReactiveTransactionManager
 */
public interface ConfigurableTransactionManager extends TransactionManager {

	/**
	 * Set the transaction execution listeners for begin/commit/rollback callbacks
	 * from this transaction manager.
	 * @see #addListener
	 */
	void setTransactionExecutionListeners(Collection<TransactionExecutionListener> listeners);

	/**
	 * Return the registered transaction execution listeners for this transaction manager.
	 * @see #setTransactionExecutionListeners
	 */
	Collection<TransactionExecutionListener> getTransactionExecutionListeners();

	/**
	 * Conveniently register the given listener for begin/commit/rollback callbacks
	 * from this transaction manager.
	 * @see #getTransactionExecutionListeners()
	 */
	default void addListener(TransactionExecutionListener listener) {
		getTransactionExecutionListeners().add(listener);
	}

}
