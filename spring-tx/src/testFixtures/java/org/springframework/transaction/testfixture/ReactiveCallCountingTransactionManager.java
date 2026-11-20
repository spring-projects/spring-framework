/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.testfixture;

import reactor.core.publisher.Mono;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.reactive.AbstractReactiveTransactionManager;
import org.springframework.transaction.reactive.GenericReactiveTransaction;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;

/**
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class ReactiveCallCountingTransactionManager extends AbstractReactiveTransactionManager {

	public TransactionDefinition lastDefinition;
	public int begun;
	public int commits;
	public int rollbacks;
	public int inflight;

	@Override
	protected Object doGetTransaction(TransactionSynchronizationManager synchronizationManager) throws TransactionException {
		return new Object();
	}

	@Override
	protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager, Object transaction, TransactionDefinition definition) throws TransactionException {
		this.lastDefinition = definition;
		++begun;
		++inflight;
		return Mono.empty();
	}

	@Override
	protected Mono<Void> doCommit(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) throws TransactionException {
		++commits;
		--inflight;
		return Mono.empty();
	}

	@Override
	protected Mono<Void> doRollback(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) throws TransactionException {
		++rollbacks;
		--inflight;
		return Mono.empty();
	}


	public void clear() {
		begun = commits = rollbacks = inflight = 0;
	}

}
