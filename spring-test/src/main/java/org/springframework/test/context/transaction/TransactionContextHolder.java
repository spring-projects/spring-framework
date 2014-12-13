/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.transaction;

import org.springframework.core.NamedInheritableThreadLocal;

/**
 * {@link InheritableThreadLocal}-based holder for the current {@link TransactionContext}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
class TransactionContextHolder {

	private static final ThreadLocal<TransactionContext> currentTransactionContext = new NamedInheritableThreadLocal<TransactionContext>(
		"Test Transaction Context");


	static TransactionContext getCurrentTransactionContext() {
		return currentTransactionContext.get();
	}

	static void setCurrentTransactionContext(TransactionContext transactionContext) {
		currentTransactionContext.set(transactionContext);
	}

	static TransactionContext removeCurrentTransactionContext() {
		synchronized (currentTransactionContext) {
			TransactionContext transactionContext = currentTransactionContext.get();
			currentTransactionContext.remove();
			return transactionContext;
		}
	}

}