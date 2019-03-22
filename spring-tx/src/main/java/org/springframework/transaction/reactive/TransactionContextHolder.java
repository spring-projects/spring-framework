/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Stack;

import org.springframework.transaction.NoTransactionException;

/**
 * Mutable holder for reactive transaction {@link TransactionContext contexts}.
 * This holder keeps references to individual {@link TransactionContext}s.
 * @author Mark Paluch
 * @since 5.2
 * @see TransactionContext
 */
class TransactionContextHolder {

	private final Stack<TransactionContext> transactionStack;


	TransactionContextHolder(Stack<TransactionContext> transactionStack) {
		this.transactionStack = transactionStack;
	}


	/**
	 * Return the current {@link TransactionContext}.
	 * @return the current {@link TransactionContext}.
	 * @throws NoTransactionException if no transaction is ongoing.
	 */
	TransactionContext currentContext() {
		TransactionContext context = (transactionStack.isEmpty() ? null : transactionStack.peek());

		if (context == null) {
			throw new NoTransactionException("No transaction in context");
		}

		return context;
	}

	/**
	 * Create a new {@link TransactionContext}.
	 * @return the new {@link TransactionContext}.
	 */
	TransactionContext createContext() {
		TransactionContext context = (transactionStack.isEmpty() ? null : transactionStack.peek());

		return (context == null ? transactionStack.push(new TransactionContext()) :
				transactionStack.push(new TransactionContext(context)));
	}

	/**
	 * Check whether the holder has a {@link TransactionContext}.
	 * @return {@literal true} if a {@link TransactionContext} is associated.
	 */
	boolean hasContext() {
		return !transactionStack.isEmpty();
	}

}
