/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.jta;

import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Default implementation of the {@link TransactionFactory} strategy interface,
 * simply wrapping a standard JTA {@link jakarta.transaction.TransactionManager}.
 *
 * <p>Does not support transaction names; simply ignores any specified name.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see jakarta.transaction.TransactionManager#setTransactionTimeout(int)
 * @see jakarta.transaction.TransactionManager#begin()
 * @see jakarta.transaction.TransactionManager#getTransaction()
 */
public class SimpleTransactionFactory implements TransactionFactory {

	private final TransactionManager transactionManager;


	/**
	 * Create a new SimpleTransactionFactory for the given TransactionManager.
	 * @param transactionManager the JTA TransactionManager to wrap
	 */
	public SimpleTransactionFactory(TransactionManager transactionManager) {
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.transactionManager = transactionManager;
	}


	@Override
	public Transaction createTransaction(@Nullable String name, int timeout) throws NotSupportedException, SystemException {
		if (timeout >= 0) {
			this.transactionManager.setTransactionTimeout(timeout);
		}
		this.transactionManager.begin();
		return new ManagedTransactionAdapter(this.transactionManager);
	}

	@Override
	public boolean supportsResourceAdapterManagedTransactions() {
		return false;
	}

}
