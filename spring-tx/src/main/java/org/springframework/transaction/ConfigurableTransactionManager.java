/*
 * Copyright 2002-2023 the original author or authors.
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
