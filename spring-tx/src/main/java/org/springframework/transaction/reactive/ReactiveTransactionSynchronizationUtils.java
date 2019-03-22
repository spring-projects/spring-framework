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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.aop.scope.ScopedObject;
import org.springframework.core.InfrastructureProxy;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility methods for triggering specific {@link ReactiveTransactionSynchronization}
 * callback methods on all currently registered synchronizations.
 *
 * @author Mark Paluch
 * @since 5.2
 * @see ReactiveTransactionSynchronization
 * @see ReactiveTransactionSynchronizationManager#getSynchronizations()
 */
public abstract class ReactiveTransactionSynchronizationUtils {

	private static final Log logger = LogFactory.getLog(ReactiveTransactionSynchronizationUtils.class);

	private static final boolean aopAvailable = ClassUtils.isPresent(
			"org.springframework.aop.scope.ScopedObject", ReactiveTransactionSynchronizationUtils.class.getClassLoader());


	/**
	 * Unwrap the given resource handle if necessary; otherwise return
	 * the given handle as-is.
	 * @see InfrastructureProxy#getWrappedObject()
	 */
	static Object unwrapResourceIfNecessary(Object resource) {
		Assert.notNull(resource, "Resource must not be null");
		Object resourceRef = resource;
		// unwrap infrastructure proxy
		if (resourceRef instanceof InfrastructureProxy) {
			resourceRef = ((InfrastructureProxy) resourceRef).getWrappedObject();
		}
		if (aopAvailable) {
			// now unwrap scoped proxy
			resourceRef = ScopedProxyUnwrapper.unwrapIfNecessary(resourceRef);
		}
		return resourceRef;
	}


	/**
	 * Trigger {@code flush} callbacks on all currently registered synchronizations.
	 * @throws RuntimeException if thrown by a {@code flush} callback
	 * @see ReactiveTransactionSynchronization#flush()
	 */
	public static Mono<Void> triggerFlush() {
		return TransactionContextManager.currentContext().flatMapIterable(TransactionContext::getSynchronizations).concatMap(ReactiveTransactionSynchronization::flush).then();
	}

	/**
	 * Trigger {@code beforeCommit} callbacks on all currently registered synchronizations.
	 *
	 * @param readOnly whether the transaction is defined as read-only transaction
	 * @throws RuntimeException if thrown by a {@code beforeCommit} callback
	 * @see ReactiveTransactionSynchronization#beforeCommit(boolean)
	 */
	public static Mono<Void> triggerBeforeCommit(boolean readOnly) {
		return TransactionContextManager.currentContext()
				.map(TransactionContext::getSynchronizations)
				.flatMap(it -> triggerBeforeCommit(it, readOnly)).then();
	}

	/**
	 * Actually invoke the {@code triggerBeforeCommit} methods of the
	 * given Spring ReactiveTransactionSynchronization objects.
	 *
	 * @param synchronizations a List of ReactiveTransactionSynchronization objects
	 * @see ReactiveTransactionSynchronization#beforeCommit(boolean)
	 */
	public static Mono<Void> triggerBeforeCommit(Collection<ReactiveTransactionSynchronization> synchronizations, boolean readOnly) {
		return Flux.fromIterable(synchronizations).concatMap(it -> it.beforeCommit(readOnly))
				.then();
	}

	/**
	 * Trigger {@code beforeCompletion} callbacks on all currently registered synchronizations.
	 * @see ReactiveTransactionSynchronization#beforeCompletion()
	 */
	public static Mono<Void> triggerBeforeCompletion() {

		return TransactionContextManager.currentContext()
				.map(TransactionContext::getSynchronizations)
				.flatMap(ReactiveTransactionSynchronizationUtils::triggerBeforeCompletion);
	}

	/**
	 * Actually invoke the {@code beforeCompletion} methods of the
	 * given Spring ReactiveTransactionSynchronization objects.
	 * @param synchronizations a List of ReactiveTransactionSynchronization objects
	 * @see ReactiveTransactionSynchronization#beforeCompletion()
	 */
	public static Mono<Void> triggerBeforeCompletion(Collection<ReactiveTransactionSynchronization> synchronizations) {

		return Flux.fromIterable(synchronizations)
				.concatMap(ReactiveTransactionSynchronization::beforeCompletion).onErrorContinue((t, o) -> {
			logger.error("TransactionSynchronization.beforeCompletion threw exception", t);
		}).then();
	}

	/**
	 * Trigger {@code afterCommit} callbacks on all currently registered synchronizations.
	 * @throws RuntimeException if thrown by a {@code afterCommit} callback
	 * @see ReactiveTransactionSynchronizationManager#getSynchronizations()
	 * @see ReactiveTransactionSynchronization#afterCommit()
	 */
	public static Mono<Void> triggerAfterCommit() {
		return TransactionContextManager.currentContext()
				.flatMap(it -> invokeAfterCommit(it.getSynchronizations()));
	}

	/**
	 * Actually invoke the {@code afterCommit} methods of the
	 * given Spring ReactiveTransactionSynchronization objects.
	 * @param synchronizations a List of ReactiveTransactionSynchronization objects
	 * @see TransactionSynchronization#afterCommit()
	 */
	public static Mono<Void> invokeAfterCommit(Collection<ReactiveTransactionSynchronization> synchronizations) {
		return Flux.fromIterable(synchronizations)
				.concatMap(ReactiveTransactionSynchronization::afterCommit)
				.then();
	}

	/**
	 * Trigger {@code afterCompletion} callbacks on all currently registered synchronizations.
	 * @param completionStatus the completion status according to the
	 * constants in the ReactiveTransactionSynchronization interface
	 * @see ReactiveTransactionSynchronizationManager#getSynchronizations()
	 * @see ReactiveTransactionSynchronization#afterCompletion(int)
	 * @see ReactiveTransactionSynchronization#STATUS_COMMITTED
	 * @see ReactiveTransactionSynchronization#STATUS_ROLLED_BACK
	 * @see ReactiveTransactionSynchronization#STATUS_UNKNOWN
	 */
	public static Mono<Void> triggerAfterCompletion(int completionStatus) {
		return TransactionContextManager.currentContext()
				.flatMap(it -> invokeAfterCompletion(it.getSynchronizations(), completionStatus));
	}

	/**
	 * Actually invoke the {@code afterCompletion} methods of the
	 * given Spring ReactiveTransactionSynchronization objects.
	 * @param synchronizations a List of ReactiveTransactionSynchronization objects
	 * @param completionStatus the completion status according to the
	 * constants in the ReactiveTransactionSynchronization interface
	 * @see ReactiveTransactionSynchronization#afterCompletion(int)
	 * @see ReactiveTransactionSynchronization#STATUS_COMMITTED
	 * @see ReactiveTransactionSynchronization#STATUS_ROLLED_BACK
	 * @see ReactiveTransactionSynchronization#STATUS_UNKNOWN
	 */
	public static Mono<Void> invokeAfterCompletion(Collection<ReactiveTransactionSynchronization> synchronizations,
												   int completionStatus) {

		return Flux.fromIterable(synchronizations).concatMap(it -> it.afterCompletion(completionStatus))
				.onErrorContinue((t, o) -> {
			logger.error("TransactionSynchronization.afterCompletion threw exception", t);
		}).then();
	}


	/**
	 * Inner class to avoid hard-coded dependency on AOP module.
	 */
	private static class ScopedProxyUnwrapper {

		static Object unwrapIfNecessary(Object resource) {
			if (resource instanceof ScopedObject) {
				return ((ScopedObject) resource).getTargetObject();
			}
			else {
				return resource;
			}
		}
	}

}
