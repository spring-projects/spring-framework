/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

import javax.transaction.xa.XAResource;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

/**
 * @author Juergen Hoeller
 * @since 31.08.2004
 */
class MockJtaTransaction implements jakarta.transaction.Transaction {

	private Synchronization synchronization;

	@Override
	public int getStatus() {
		return Status.STATUS_ACTIVE;
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		this.synchronization = synchronization;
	}

	public Synchronization getSynchronization() {
		return synchronization;
	}

	@Override
	public boolean enlistResource(XAResource xaResource) {
		return false;
	}

	@Override
	public boolean delistResource(XAResource xaResource, int i) {
		return false;
	}

	@Override
	public void commit() {
	}

	@Override
	public void rollback() {
	}

	@Override
	public void setRollbackOnly() {
	}

}
