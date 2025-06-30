/*
 * Copyright 2002-present the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * Operator class that simplifies programmatic transaction demarcation and
 * transaction exception handling.
 *
 * <p>The central method is {@link #transactional}, supporting transactional wrapping
 * of functional sequences code that. This operator handles the transaction lifecycle
 * and possible exceptions such that neither the ReactiveTransactionCallback
 * implementation nor the calling code needs to explicitly handle transactions.
 *
 * <p>Typical usage: Allows for writing low-level data access objects that use
 * resources such as database connections but are not transaction-aware themselves.
 * Instead, they can implicitly participate in transactions handled by higher-level
 * application services utilizing this class, making calls to the low-level
 * services via an inner-class callback object.
 *
 * <p><strong>Note:</strong> Transactional Publishers should avoid Subscription
 * cancellation. See the
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#tx-prog-operator-cancel">Cancel Signals</a>
 * section of the Spring Framework reference for more details.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @author Enric Sala
 * @since 5.2
 * @see #execute
 * @see ReactiveTransactionManager
 */
public interface TransactionalOperator {

	/**
	 * Wrap the functional sequence specified by the given Flux within a transaction.
	 * @param flux the Flux that should be executed within the transaction
	 * @return a result publisher returned by the callback, or {@code null} if none
	 * @throws TransactionException in case of initialization, rollback, or system errors
	 * @throws RuntimeException if thrown by the TransactionCallback
	 */
	default <T> Flux<T> transactional(Flux<T> flux) {
		return execute(it -> flux);
	}

	/**
	 * Wrap the functional sequence specified by the given Mono within a transaction.
	 * @param mono the Mono that should be executed within the transaction
	 * @return a result publisher returned by the callback
	 * @throws TransactionException in case of initialization, rollback, or system errors
	 * @throws RuntimeException if thrown by the TransactionCallback
	 */
	default <T> Mono<T> transactional(Mono<T> mono) {
		return execute(it -> mono).singleOrEmpty();
	}

	/**
	 * Execute the action specified by the given callback object within a transaction.
	 * <p>Allows for returning a result object created within the transaction, that is,
	 * a domain object or a collection of domain objects. A RuntimeException thrown
	 * by the callback is treated as a fatal exception that enforces a rollback.
	 * Such an exception gets propagated to the caller of the template.
	 * @param action the callback object that specifies the transactional action
	 * @return a result object returned by the callback
	 * @throws TransactionException in case of initialization, rollback, or system errors
	 * @throws RuntimeException if thrown by the TransactionCallback
	 */
	<T> Flux<T> execute(TransactionCallback<T> action) throws TransactionException;


	// Static builder methods

	/**
	 * Create a new {@link TransactionalOperator} using {@link ReactiveTransactionManager},
	 * using a default transaction.
	 * @param transactionManager the transaction management strategy to be used
	 * @return the transactional operator
	 */
	static TransactionalOperator create(ReactiveTransactionManager transactionManager){
		return create(transactionManager, TransactionDefinition.withDefaults());
	}

	/**
	 * Create a new {@link TransactionalOperator} using {@link ReactiveTransactionManager}
	 * and {@link TransactionDefinition}.
	 * @param transactionManager the transaction management strategy to be used
	 * @param transactionDefinition the transaction definition to apply
	 * @return the transactional operator
	 */
	static TransactionalOperator create(
			ReactiveTransactionManager transactionManager, TransactionDefinition transactionDefinition){

		return new TransactionalOperatorImpl(transactionManager, transactionDefinition);
	}

}
