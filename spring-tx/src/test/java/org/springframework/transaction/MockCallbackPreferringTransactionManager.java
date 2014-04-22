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

package org.springframework.transaction;

import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

/**
 * @author Juergen Hoeller
 */
public class MockCallbackPreferringTransactionManager implements CallbackPreferringPlatformTransactionManager {

	private TransactionDefinition definition;

	private TransactionStatus status;


	@Override
	public <T> T execute(TransactionDefinition definition, TransactionCallback<T> callback) throws TransactionException {
		this.definition = definition;
		this.status = new SimpleTransactionStatus();
		return callback.doInTransaction(this.status);
	}

	public TransactionDefinition getDefinition() {
		return this.definition;
	}

	public TransactionStatus getStatus() {
		return this.status;
	}


	@Override
	public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void commit(TransactionStatus status) throws TransactionException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void rollback(TransactionStatus status) throws TransactionException {
		throw new UnsupportedOperationException();
	}

}
