/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.reactive;

import java.util.Deque;

import org.springframework.transaction.NoTransactionException;

/**
 * Mutable holder for reactive transaction {@link TransactionContext contexts}.
 * This holder keeps references to individual {@link TransactionContext}s.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see TransactionContext
 */
final class TransactionContextHolder {

	private final Deque<TransactionContext> transactionStack;


	TransactionContextHolder(Deque<TransactionContext> transactionStack) {
		this.transactionStack = transactionStack;
	}


	/**
	 * Return the current {@link TransactionContext}.
	 * @throws NoTransactionException if no transaction is ongoing
	 */
	TransactionContext currentContext() {
		TransactionContext context = this.transactionStack.peek();
		if (context == null) {
			throw new NoTransactionException("No transaction in context");
		}
		return context;
	}

	/**
	 * Create a new {@link TransactionContext}.
	 */
	TransactionContext createContext() {
		TransactionContext context = this.transactionStack.peek();
		if (context != null) {
			context = new TransactionContext(context);
		}
		else {
			context = new TransactionContext();
		}
		this.transactionStack.push(context);
		return context;
	}

	/**
	 * Check whether the holder has a {@link TransactionContext}.
	 * @return {@literal true} if a {@link TransactionContext} is associated
	 */
	boolean hasContext() {
		return !this.transactionStack.isEmpty();
	}

}
