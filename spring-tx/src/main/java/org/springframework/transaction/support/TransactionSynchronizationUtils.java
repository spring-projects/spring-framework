/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.support;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.scope.ScopedObject;
import org.springframework.core.InfrastructureProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility methods for triggering specific {@link TransactionSynchronization}
 * callback methods on all currently registered synchronizations.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see TransactionSynchronization
 * @see TransactionSynchronizationManager#getSynchronizations()
 */
public abstract class TransactionSynchronizationUtils {

	private static final Log logger = LogFactory.getLog(TransactionSynchronizationUtils.class);

	private static final boolean aopAvailable = ClassUtils.isPresent(
			"org.springframework.aop.scope.ScopedObject", TransactionSynchronizationUtils.class.getClassLoader());


	/**
	 * Check whether the given resource transaction managers refers to the given
	 * (underlying) resource factory.
	 * @see ResourceTransactionManager#getResourceFactory()
	 * @see org.springframework.core.InfrastructureProxy#getWrappedObject()
	 */
	public static boolean sameResourceFactory(ResourceTransactionManager tm, Object resourceFactory) {
		return unwrapResourceIfNecessary(tm.getResourceFactory()).equals(unwrapResourceIfNecessary(resourceFactory));
	}

	/**
	 * Unwrap the given resource handle if necessary; otherwise return
	 * the given handle as-is.
	 * @see org.springframework.core.InfrastructureProxy#getWrappedObject()
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
	 * @see TransactionSynchronization#flush()
	 */
	public static void triggerFlush() {
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			synchronization.flush();
		}
	}

	/**
	 * Trigger {@code beforeCommit} callbacks on all currently registered synchronizations.
	 * @param readOnly whether the transaction is defined as read-only transaction
	 * @throws RuntimeException if thrown by a {@code beforeCommit} callback
	 * @see TransactionSynchronization#beforeCommit(boolean)
	 */
	public static void triggerBeforeCommit(boolean readOnly) {
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			synchronization.beforeCommit(readOnly);
		}
	}

	/**
	 * Trigger {@code beforeCompletion} callbacks on all currently registered synchronizations.
	 * @see TransactionSynchronization#beforeCompletion()
	 */
	public static void triggerBeforeCompletion() {
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			try {
				synchronization.beforeCompletion();
			}
			catch (Throwable tsex) {
				logger.error("TransactionSynchronization.beforeCompletion threw exception", tsex);
			}
		}
	}

	/**
	 * Trigger {@code afterCommit} callbacks on all currently registered synchronizations.
	 * @throws RuntimeException if thrown by a {@code afterCommit} callback
	 * @see TransactionSynchronizationManager#getSynchronizations()
	 * @see TransactionSynchronization#afterCommit()
	 */
	public static void triggerAfterCommit() {
		invokeAfterCommit(TransactionSynchronizationManager.getSynchronizations());
	}

	/**
	 * Actually invoke the {@code afterCommit} methods of the
	 * given Spring TransactionSynchronization objects.
	 * @param synchronizations a List of TransactionSynchronization objects
	 * @see TransactionSynchronization#afterCommit()
	 */
	public static void invokeAfterCommit(@Nullable List<TransactionSynchronization> synchronizations) {
		if (synchronizations != null) {
			for (TransactionSynchronization synchronization : synchronizations) {
				synchronization.afterCommit();
			}
		}
	}

	/**
	 * Trigger {@code afterCompletion} callbacks on all currently registered synchronizations.
	 * @param completionStatus the completion status according to the
	 * constants in the TransactionSynchronization interface
	 * @see TransactionSynchronizationManager#getSynchronizations()
	 * @see TransactionSynchronization#afterCompletion(int)
	 * @see TransactionSynchronization#STATUS_COMMITTED
	 * @see TransactionSynchronization#STATUS_ROLLED_BACK
	 * @see TransactionSynchronization#STATUS_UNKNOWN
	 */
	public static void triggerAfterCompletion(int completionStatus) {
		List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
		invokeAfterCompletion(synchronizations, completionStatus);
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
	public static void invokeAfterCompletion(@Nullable List<TransactionSynchronization> synchronizations,
			int completionStatus) {

		if (synchronizations != null) {
			for (TransactionSynchronization synchronization : synchronizations) {
				try {
					synchronization.afterCompletion(completionStatus);
				}
				catch (Throwable tsex) {
					logger.error("TransactionSynchronization.afterCompletion threw exception", tsex);
				}
			}
		}
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
