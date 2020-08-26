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

package org.springframework.transaction.support;

import java.util.function.Consumer;

import org.springframework.lang.Nullable;
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
	@Nullable
	public <T> T execute(TransactionCallback<T> action) throws TransactionException {
		return action.doInTransaction(new SimpleTransactionStatus(false));
	}

	@Override
	public void executeWithoutResult(Consumer<TransactionStatus> action) throws TransactionException {
		action.accept(new SimpleTransactionStatus(false));
	}

}
