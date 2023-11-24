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

package org.springframework.transaction.reactive;

import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/**
 * Test implementation of a {@link ReactiveTransactionManager}.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
class ReactiveTestTransactionManager extends AbstractReactiveTransactionManager {

	private static final Object TRANSACTION = "transaction";

	private final boolean existingTransaction;

	private final boolean canCreateTransaction;

	@Nullable
	private Function<String, RuntimeException> forceFailOnCommit;

	@Nullable
	private Function<String, RuntimeException> forceFailOnRollback;

	protected boolean begin = false;

	protected boolean commit = false;

	protected boolean rollback = false;

	protected boolean rollbackOnly = false;

	protected boolean cleanup = false;


	ReactiveTestTransactionManager(boolean existingTransaction, boolean canCreateTransaction) {
		this.existingTransaction = existingTransaction;
		this.canCreateTransaction = canCreateTransaction;
	}

	ReactiveTestTransactionManager(boolean existingTransaction, @Nullable Function<String, RuntimeException> forceFailOnCommit, @Nullable Function<String, RuntimeException> forceFailOnRollback) {
		this.existingTransaction = existingTransaction;
		this.canCreateTransaction = true;
		this.forceFailOnCommit = forceFailOnCommit;
		this.forceFailOnRollback = forceFailOnRollback;
	}


	@Override
	protected Object doGetTransaction(TransactionSynchronizationManager synchronizationManager) {
		return TRANSACTION;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		return this.existingTransaction;
	}

	@Override
	protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager, Object transaction, TransactionDefinition definition) {
		if (!TRANSACTION.equals(transaction)) {
			return Mono.error(new IllegalArgumentException("Not the same transaction object"));
		}
		if (!this.canCreateTransaction) {
			return Mono.error(new CannotCreateTransactionException("Cannot create transaction"));
		}
		return Mono.fromRunnable(() -> this.begin = true);
	}

	@Override
	protected Mono<Void> doCommit(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
		if (!TRANSACTION.equals(status.getTransaction())) {
			return Mono.error(new IllegalArgumentException("Not the same transaction object"));
		}
		return Mono.fromRunnable(() -> {
			this.commit = true;
			if (this.forceFailOnCommit != null) {
				throw this.forceFailOnCommit.apply("Forced failure on commit");
			}
		});
	}

	@Override
	protected Mono<Void> doRollback(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
		if (!TRANSACTION.equals(status.getTransaction())) {
			return Mono.error(new IllegalArgumentException("Not the same transaction object"));
		}
		return Mono.fromRunnable(() -> {
			this.rollback = true;
			if (this.forceFailOnRollback != null) {
				throw this.forceFailOnRollback.apply("Forced failure on rollback");
			}
		});
	}

	@Override
	protected Mono<Void> doSetRollbackOnly(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
		if (!TRANSACTION.equals(status.getTransaction())) {
			return Mono.error(new IllegalArgumentException("Not the same transaction object"));
		}
		return Mono.fromRunnable(() -> this.rollbackOnly = true);
	}

	@Override
	protected Mono<Void> doCleanupAfterCompletion(TransactionSynchronizationManager synchronizationManager, Object transaction) {
		return Mono.fromRunnable(() -> this.cleanup = true);
	}
}
