/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.support;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * A {@link TransactionOperations} implementation which executes a given
 * {@link TransactionCallback} without an actual transaction.
 *
 * @author Juergen Hoeller
 * @since 5.2
 * @see TransactionOperations#withoutTransaction()
 */
final class WithoutTransactionOperations implements TransactionOperations {

	static final WithoutTransactionOperations INSTANCE = new WithoutTransactionOperations();


	private WithoutTransactionOperations() {
	}


	@Override
	public <T extends @Nullable Object> T execute(TransactionCallback<T> action) throws TransactionException {
		return action.doInTransaction(new SimpleTransactionStatus(false));
	}

	@Override
	public void executeWithoutResult(Consumer<TransactionStatus> action) throws TransactionException {
		action.accept(new SimpleTransactionStatus(false));
	}

}
