/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.jta;

import javax.transaction.xa.XAResource;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.springframework.util.Assert;

/**
 * Adapter for a managed JTA Transaction handle, taking a JTA
 * {@link jakarta.transaction.TransactionManager} reference and creating
 * a JTA {@link jakarta.transaction.Transaction} handle for it.
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
