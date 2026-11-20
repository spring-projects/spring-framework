/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.testfixture;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class CallCountingTransactionManager extends AbstractPlatformTransactionManager {

	public TransactionDefinition lastDefinition;
	public int begun;
	public int commits;
	public int rollbacks;
	public int inflight;

	@Override
	protected Object doGetTransaction() {
		return new Object();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		this.lastDefinition = definition;
		++begun;
		++inflight;
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		++commits;
		--inflight;
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		++rollbacks;
		--inflight;
	}

	public void clear() {
		begun = commits = rollbacks = inflight = 0;
	}

}
