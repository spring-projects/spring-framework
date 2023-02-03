/*
 * Copyright 2002-2022 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.Assert;

/**
 * Operator class that simplifies programmatic transaction demarcation and
 * transaction exception handling.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see #execute
 * @see ReactiveTransactionManager
 */
final class TransactionalOperatorImpl implements TransactionalOperator {

	private static final Log logger = LogFactory.getLog(TransactionalOperatorImpl.class);

	private final ReactiveTransactionManager transactionManager;

	private final TransactionDefinition transactionDefinition;


	/**
	 * Construct a new TransactionTemplate using the given transaction manager,
	 * taking its default settings from the given transaction definition.
	 * @param transactionManager the transaction management strategy to be used
	 * @param transactionDefinition the transaction definition to copy the
	 * default settings from. Local properties can still be set to change values.
	 */
	TransactionalOperatorImpl(ReactiveTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
		Assert.notNull(transactionManager, "ReactiveTransactionManager must not be null");
		Assert.notNull(transactionDefinition, "TransactionDefinition must not be null");
		this.transactionManager = transactionManager;
		this.transactionDefinition = transactionDefinition;
	}


	/**
	 * Return the transaction management strategy to be used.
	 */
	public ReactiveTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	@Override
	public <T> Mono<T> transactional(Mono<T> mono) {
		return TransactionContextManager.currentContext().flatMap(context -> {
			Mono<ReactiveTransaction> status = this.transactionManager.getReactiveTransaction(this.transactionDefinition);
			// This is an around advice: Invoke the next interceptor in the chain.
			// This will normally result in a target object being invoked.
			// Need re-wrapping of ReactiveTransaction until we get hold of the exception
			// through usingWhen.
			return status.flatMap(it -> Mono.usingWhen(Mono.just(it), ignore -> mono,
					this.transactionManager::commit, (res, err) -> Mono.empty(), this.transactionManager::rollback)
					.onErrorResume(ex -> rollbackOnException(it, ex).then(Mono.error(ex))));
		})
		.contextWrite(TransactionContextManager.getOrCreateContext())
		.contextWrite(TransactionContextManager.getOrCreateContextHolder());
	}

	@Override
	public <T> Flux<T> execute(TransactionCallback<T> action) throws TransactionException {
		return TransactionContextManager.currentContext().flatMapMany(context -> {
			Mono<ReactiveTransaction> status = this.transactionManager.getReactiveTransaction(this.transactionDefinition);
			// This is an around advice: Invoke the next interceptor in the chain.
			// This will normally result in a target object being invoked.
			// Need re-wrapping of ReactiveTransaction until we get hold of the exception
			// through usingWhen.
			return status.flatMapMany(it -> Flux
					.usingWhen(
							Mono.just(it),
							action::doInTransaction,
							this.transactionManager::commit,
							(tx, ex) -> Mono.empty(),
							this.transactionManager::rollback)
					.onErrorResume(ex ->
							rollbackOnException(it, ex).then(Mono.error(ex))));
		})
		.contextWrite(TransactionContextManager.getOrCreateContext())
		.contextWrite(TransactionContextManager.getOrCreateContextHolder());
	}

	/**
	 * Perform a rollback, handling rollback exceptions properly.
	 * @param status object representing the transaction
	 * @param ex the thrown application exception or error
	 * @throws TransactionException in case of a rollback error
	 */
	private Mono<Void> rollbackOnException(ReactiveTransaction status, Throwable ex) throws TransactionException {
		logger.debug("Initiating transaction rollback on application exception", ex);
		return this.transactionManager.rollback(status).onErrorMap(ex2 -> {
					logger.error("Application exception overridden by rollback exception", ex);
					if (ex2 instanceof TransactionSystemException) {
						((TransactionSystemException) ex2).initApplicationException(ex);
					}
					return ex2;
				}
		);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (super.equals(other) && (!(other instanceof TransactionalOperatorImpl) ||
				getTransactionManager() == ((TransactionalOperatorImpl) other).getTransactionManager())));
	}

	@Override
	public int hashCode() {
		return getTransactionManager().hashCode();
	}

}
