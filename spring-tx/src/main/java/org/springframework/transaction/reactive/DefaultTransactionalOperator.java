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

package org.springframework.transaction.reactive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.ReactiveTransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;

/**
 * Operator class that simplifies programmatic transaction demarcation and
 * transaction exception handling.
 *
 * @author Mark Paluch
 * @since 5.2
 * @see #execute
 * @see ReactiveTransactionManager
 */
@SuppressWarnings("serial")
class DefaultTransactionalOperator extends DefaultTransactionDefinition
		implements TransactionalOperator {

	private final Log logger = LogFactory.getLog(getClass());

	private final ReactiveTransactionManager transactionManager;

	/**
	 * Construct a new DefaultTransactionalOperator using the given transaction manager.
	 * @param transactionManager the transaction management strategy to be used
	 */
	DefaultTransactionalOperator(ReactiveTransactionManager transactionManager) {
		Assert.notNull(transactionManager, "ReactiveTransactionManager must not be null");
		this.transactionManager = transactionManager;
	}

	/**
	 * Construct a new TransactionTemplate using the given transaction manager,
	 * taking its default settings from the given transaction definition.
	 * @param transactionManager the transaction management strategy to be used
	 * @param transactionDefinition the transaction definition to copy the
	 * default settings from. Local properties can still be set to change values.
	 */
	DefaultTransactionalOperator(ReactiveTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
		super(transactionDefinition);
		Assert.notNull(transactionManager, "ReactiveTransactionManager must not be null");
		this.transactionManager = transactionManager;
	}


	/**
	 * Return the transaction management strategy to be used.
	 */
	public ReactiveTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	@Override
	public <T> Flux<T> execute(ReactiveTransactionCallback<T> action) throws TransactionException {

		return TransactionContextManager.currentContext().flatMapMany(context -> {

			Mono<ReactiveTransactionStatus> status = this.transactionManager.getTransaction(this);

			return status.flatMapMany(it -> {

				// This is an around advice: Invoke the next interceptor in the chain.
				// This will normally result in a target object being invoked.
				Flux<Object> retVal = Flux.from(action.doInTransaction(it));

				return retVal.onErrorResume(ex -> {
					// Transactional code threw application exception -> rollback
					return rollbackOnException(it, ex).then(Mono.error(ex));
				}).materialize().flatMap(signal -> {

					if (signal.isOnComplete()) {
						return transactionManager.commit(it).materialize();
					}

					return Mono.just(signal);
				}).<T>dematerialize();
			});
		})
		.subscriberContext(TransactionContextManager.getOrCreateContext())
		.subscriberContext(TransactionContextManager.getOrCreateContextHolder());
	}

	/**
	 * Perform a rollback, handling rollback exceptions properly.
	 * @param status object representing the transaction
	 * @param ex the thrown application exception or error
	 * @throws TransactionException in case of a rollback error
	 */
	private Mono<Void> rollbackOnException(ReactiveTransactionStatus status, Throwable ex) throws TransactionException {

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
	public boolean equals(Object other) {
		return (this == other || (super.equals(other) && (!(other instanceof DefaultTransactionalOperator) ||
				getTransactionManager() == ((DefaultTransactionalOperator) other).getTransactionManager())));
	}

}
