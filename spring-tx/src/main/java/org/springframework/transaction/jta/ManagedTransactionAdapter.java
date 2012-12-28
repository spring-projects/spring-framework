/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.jta;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.springframework.util.Assert;

/**
 * Adapter for a managed JTA Transaction handle, taking a JTA
 * {@link javax.transaction.TransactionManager} reference and creating
 * a JTA {@link javax.transaction.Transaction} handle for it.
 *
 * @author Juergen Hoeller
 * @since 3.0.2
 */
public class ManagedTransactionAdapter implements Transaction {

	private final TransactionManager transactionManager;


	/**
	 * Create a new ManagedTransactionAdapter for the given TransactionManager.
	 * @param transactionManager the JTA TransactionManager to wrap
	 */
	public ManagedTransactionAdapter(TransactionManager transactionManager) throws SystemException {
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.transactionManager = transactionManager;
	}

	/**
	 * Return the JTA TransactionManager that this adapter delegates to.
	 */
	public final TransactionManager getTransactionManager() {
		return this.transactionManager;
	}


	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, SystemException {
		this.transactionManager.commit();
	}

	@Override
	public void rollback() throws SystemException {
		this.transactionManager.rollback();
	}

	@Override
	public void setRollbackOnly() throws SystemException {
		this.transactionManager.setRollbackOnly();
	}

	@Override
	public int getStatus() throws SystemException {
		return this.transactionManager.getStatus();
	}

	@Override
	public boolean enlistResource(XAResource xaRes) throws RollbackException, SystemException {
		return this.transactionManager.getTransaction().enlistResource(xaRes);
	}

	@Override
	public boolean delistResource(XAResource xaRes, int flag) throws SystemException {
		return this.transactionManager.getTransaction().delistResource(xaRes, flag);
	}

	@Override
	public void registerSynchronization(Synchronization sync) throws RollbackException, SystemException {
		this.transactionManager.getTransaction().registerSynchronization(sync);
	}

}
