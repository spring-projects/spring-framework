/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.interceptor;

import org.jspecify.annotations.Nullable;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * Used for testing only (for example, when we must replace the
 * behavior of a PlatformTransactionManager bean we don't have access to).
 *
 * <p>Allows behavior of an entire class to change with static delegate change.
 * Not multi-threaded.
 *
 * @author Rod Johnson
 * @since 26.04.2003
 */
class PlatformTransactionManagerFacade implements PlatformTransactionManager {

	/**
	 * This member can be changed to change behavior class-wide.
	 */
	public static PlatformTransactionManager delegate;

	@Override
	public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) {
		return delegate.getTransaction(definition);
	}

	@Override
	public void commit(TransactionStatus status) {
		delegate.commit(status);
	}

	@Override
	public void rollback(TransactionStatus status) {
		delegate.rollback(status);
	}

}
