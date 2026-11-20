/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.testfixture;

import org.jspecify.annotations.Nullable;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
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
	public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
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
