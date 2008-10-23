/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.InfrastructureProxy;
import org.springframework.util.Assert;

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
		return (resource instanceof InfrastructureProxy ? ((InfrastructureProxy) resource).getWrappedObject() : resource);
	}


	/**
	 * Trigger <code>beforeCommit</code> callbacks on all currently registered synchronizations.
	 * @param readOnly whether the transaction is defined as read-only transaction
	 * @throws RuntimeException if thrown by a <code>beforeCommit</code> callback
	 * @see TransactionSynchronization#beforeCommit(boolean)
	 */
	public static void triggerBeforeCommit(boolean readOnly) {
		for (Iterator it = TransactionSynchronizationManager.getSynchronizations().iterator(); it.hasNext();) {
			TransactionSynchronization synchronization = (TransactionSynchronization) it.next();
			synchronization.beforeCommit(readOnly);
		}
	}

	/**
	 * Trigger <code>beforeCompletion</code> callbacks on all currently registered synchronizations.
	 * @see TransactionSynchronization#beforeCompletion()
	 */
	public static void triggerBeforeCompletion() {
		for (Iterator it = TransactionSynchronizationManager.getSynchronizations().iterator(); it.hasNext();) {
			TransactionSynchronization synchronization = (TransactionSynchronization) it.next();
			try {
				synchronization.beforeCompletion();
			}
			catch (Throwable tsex) {
				logger.error("TransactionSynchronization.beforeCompletion threw exception", tsex);
			}
		}
	}

	/**
	 * Trigger <code>afterCommit</code> callbacks on all currently registered synchronizations.
	 * @throws RuntimeException if thrown by a <code>afterCommit</code> callback
	 * @see TransactionSynchronizationManager#getSynchronizations()
	 * @see TransactionSynchronization#afterCommit()
	 */
	public static void triggerAfterCommit() {
		List synchronizations = TransactionSynchronizationManager.getSynchronizations();
		invokeAfterCommit(synchronizations);
	}

	/**
	 * Actually invoke the <code>afterCommit</code> methods of the
	 * given Spring TransactionSynchronization objects.
	 * @param synchronizations List of TransactionSynchronization objects
	 * @see TransactionSynchronization#afterCommit()
	 */
	public static void invokeAfterCommit(List synchronizations) {
		if (synchronizations != null) {
			for (Iterator it = synchronizations.iterator(); it.hasNext();) {
				TransactionSynchronization synchronization = (TransactionSynchronization) it.next();
				try {
					synchronization.afterCommit();
				}
				catch (AbstractMethodError tserr) {
					if (logger.isDebugEnabled()) {
						logger.debug("Spring 2.0's TransactionSynchronization.afterCommit method not implemented in " +
								"synchronization class [" + synchronization.getClass().getName() + "]", tserr);
					}
				}
			}
		}
	}

	/**
	 * Trigger <code>afterCompletion</code> callbacks on all currently registered synchronizations.
	 * @see TransactionSynchronizationManager#getSynchronizations()
	 * @param completionStatus the completion status according to the
	 * constants in the TransactionSynchronization interface
	 * @see TransactionSynchronization#afterCompletion(int)
	 * @see TransactionSynchronization#STATUS_COMMITTED
	 * @see TransactionSynchronization#STATUS_ROLLED_BACK
	 * @see TransactionSynchronization#STATUS_UNKNOWN
	 */
	public static void triggerAfterCompletion(int completionStatus) {
		List synchronizations = TransactionSynchronizationManager.getSynchronizations();
		invokeAfterCompletion(synchronizations, completionStatus);
	}

	/**
	 * Actually invoke the <code>afterCompletion</code> methods of the
	 * given Spring TransactionSynchronization objects.
	 * @param synchronizations List of TransactionSynchronization objects
	 * @param completionStatus the completion status according to the
	 * constants in the TransactionSynchronization interface
	 * @see TransactionSynchronization#afterCompletion(int)
	 * @see TransactionSynchronization#STATUS_COMMITTED
	 * @see TransactionSynchronization#STATUS_ROLLED_BACK
	 * @see TransactionSynchronization#STATUS_UNKNOWN
	 */
	public static void invokeAfterCompletion(List synchronizations, int completionStatus) {
		if (synchronizations != null) {
			for (Iterator it = synchronizations.iterator(); it.hasNext();) {
				TransactionSynchronization synchronization = (TransactionSynchronization) it.next();
				try {
					synchronization.afterCompletion(completionStatus);
				}
				catch (Throwable tsex) {
					logger.error("TransactionSynchronization.afterCompletion threw exception", tsex);
				}
			}
		}
	}

}
