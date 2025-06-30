/*
 * Copyright 2002-present the original author or authors.
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
