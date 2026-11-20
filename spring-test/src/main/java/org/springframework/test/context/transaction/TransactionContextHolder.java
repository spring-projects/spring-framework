/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.transaction;

import org.jspecify.annotations.Nullable;

import org.springframework.core.NamedInheritableThreadLocal;

/**
 * {@link InheritableThreadLocal}-based holder for the current {@link TransactionContext}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
final class TransactionContextHolder {

	private static final ThreadLocal<TransactionContext> currentTransactionContext =
			new NamedInheritableThreadLocal<>("Test Transaction Context");


	private TransactionContextHolder() {
	}


	static void setCurrentTransactionContext(TransactionContext transactionContext) {
		currentTransactionContext.set(transactionContext);
	}

	static @Nullable TransactionContext getCurrentTransactionContext() {
		return currentTransactionContext.get();
	}

	static @Nullable TransactionContext removeCurrentTransactionContext() {
		TransactionContext transactionContext = currentTransactionContext.get();
		currentTransactionContext.remove();
		return transactionContext;
	}

}
