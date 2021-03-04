/*
 * Copyright 2002-2020 the original author or authors.
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
 * Utility methods for triggering specific {@link TransactionSynchronization}
 * callback methods on all currently registered synchronizations.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see TransactionSynchronization
 * @see TransactionSynchronizationManager#getSynchronizations()
 */
abstract class TransactionSynchronizationUtils {

	private static final Log logger = LogFactory.getLog(TransactionSynchronizationUtils.class);

	private static final boolean aopAvailable = ClassUtils.isPresent(
			"org.springframework.aop.scope.ScopedObject", TransactionSynchronizationUtils.class.getClassLoader());


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
	 * given Spring TransactionSynchronization objects.
	 * @param synchronizations a List of TransactionSynchronization objects
	 * @see TransactionSynchronization#beforeCommit(boolean)
	 */
	public static Mono<Void> triggerBeforeCommit(Collection<TransactionSynchronization> synchronizations, boolean readOnly) {
		return Flux.fromIterable(synchronizations).concatMap(it -> it.beforeCommit(readOnly)).then();
	}

	/**
	 * Actually invoke the {@code beforeCompletion} methods of the
	 * given Spring TransactionSynchronization objects.
	 * @param synchronizations a List of TransactionSynchronization objects
	 * @see TransactionSynchronization#beforeCompletion()
	 */
	public static Mono<Void> triggerBeforeCompletion(Collection<TransactionSynchronization> synchronizations) {
		return Flux.fromIterable(synchronizations)
				.concatMap(TransactionSynchronization::beforeCompletion).onErrorContinue((t, o) ->
						logger.debug("TransactionSynchronization.beforeCompletion threw exception", t)).then();
	}

	/**
	 * Actually invoke the {@code afterCommit} methods of the
	 * given Spring TransactionSynchronization objects.
	 * @param synchronizations a List of TransactionSynchronization objects
	 * @see TransactionSynchronization#afterCommit()
	 */
	public static Mono<Void> invokeAfterCommit(Collection<TransactionSynchronization> synchronizations) {
		return Flux.fromIterable(synchronizations)
				.concatMap(TransactionSynchronization::afterCommit)
				.then();
	}

	/**
	 * Actually invoke the {@code afterCompletion} methods of the
	 * given Spring TransactionSynchronization objects.
	 * @param synchronizations a List of TransactionSynchronization objects
	 * @param completionStatus the completion status according to the
	 * constants in the TransactionSynchronization interface
	 * @see TransactionSynchronization#afterCompletion(int)
	 * @see TransactionSynchronization#STATUS_COMMITTED
	 * @see TransactionSynchronization#STATUS_ROLLED_BACK
	 * @see TransactionSynchronization#STATUS_UNKNOWN
	 */
	public static Mono<Void> invokeAfterCompletion(
			Collection<TransactionSynchronization> synchronizations, int completionStatus) {

		return Flux.fromIterable(synchronizations).concatMap(it -> it.afterCompletion(completionStatus))
				.onErrorContinue((t, o) -> logger.debug("TransactionSynchronization.afterCompletion threw exception", t)).then();
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
