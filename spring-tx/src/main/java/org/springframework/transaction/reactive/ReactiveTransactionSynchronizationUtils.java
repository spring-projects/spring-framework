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
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility methods for triggering specific {@link ReactiveTransactionSynchronization}
 * callback methods on all currently registered synchronizations.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see ReactiveTransactionSynchronization
 * @see ReactiveTransactionSynchronizationManager#getSynchronizations()
 */
abstract class ReactiveTransactionSynchronizationUtils {

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
	 * Actually invoke the {@code triggerBeforeCommit} methods of the
	 * given Spring ReactiveTransactionSynchronization objects.
	 * @param synchronizations a List of ReactiveTransactionSynchronization objects
	 * @see ReactiveTransactionSynchronization#beforeCommit(boolean)
	 */
	public static Mono<Void> triggerBeforeCommit(Collection<ReactiveTransactionSynchronization> synchronizations, boolean readOnly) {
		return Flux.fromIterable(synchronizations).concatMap(it -> it.beforeCommit(readOnly)).then();
	}

	/**
	 * Actually invoke the {@code beforeCompletion} methods of the
	 * given Spring ReactiveTransactionSynchronization objects.
	 * @param synchronizations a List of ReactiveTransactionSynchronization objects
	 * @see ReactiveTransactionSynchronization#beforeCompletion()
	 */
	public static Mono<Void> triggerBeforeCompletion(Collection<ReactiveTransactionSynchronization> synchronizations) {
		return Flux.fromIterable(synchronizations)
				.concatMap(ReactiveTransactionSynchronization::beforeCompletion).onErrorContinue((t, o) ->
						logger.error("TransactionSynchronization.beforeCompletion threw exception", t)).then();
	}

	/**
	 * Actually invoke the {@code afterCommit} methods of the
	 * given Spring ReactiveTransactionSynchronization objects.
	 * @param synchronizations a List of ReactiveTransactionSynchronization objects
	 * @see ReactiveTransactionSynchronization#afterCommit()
	 */
	public static Mono<Void> invokeAfterCommit(Collection<ReactiveTransactionSynchronization> synchronizations) {
		return Flux.fromIterable(synchronizations)
				.concatMap(ReactiveTransactionSynchronization::afterCommit)
				.then();
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
	public static Mono<Void> invokeAfterCompletion(
			Collection<ReactiveTransactionSynchronization> synchronizations, int completionStatus) {

		return Flux.fromIterable(synchronizations).concatMap(it -> it.afterCompletion(completionStatus))
				.onErrorContinue((t, o) -> logger.error("TransactionSynchronization.afterCompletion threw exception", t)).then();
	}


	/**
	 * Inner class to avoid hard-coded dependency on AOP module.
	 */
	private static class ScopedProxyUnwrapper {

		public static Object unwrapIfNecessary(Object resource) {
			if (resource instanceof ScopedObject) {
				return ((ScopedObject) resource).getTargetObject();
			}
			else {
				return resource;
			}
		}
	}

}
