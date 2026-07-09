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

package org.springframework.transaction.interceptor;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.context.event.MethodFailureEvent;
import org.springframework.transaction.TransactionExecution;

/**
 * Event published for every exception encountered that triggers a transaction rollback
 * through a proxy-triggered method invocation or a reactive publisher returned from it.
 * Can be listened to via an {@code ApplicationListener<MethodRollbackEvent>} bean or
 * an {@code @EventListener(MethodRollbackEvent.class)} method.
 *
 * <p>Note: This event gets published right <i>before</i> the actual transaction rollback.
 * As a consequence, the exposed {@link #getTransaction() transaction} reflects the state
 * of the transaction right before the rollback.
 *
 * @author Juergen Hoeller
 * @since 7.0.3
 * @see TransactionInterceptor
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.event.EventListener
 */
@SuppressWarnings("serial")
public class MethodRollbackEvent extends MethodFailureEvent {

	private final TransactionExecution transaction;


	/**
	 * Create a new event for the given rolled-back method invocation.
	 * @param invocation the transactional method invocation
	 * @param failure the exception encountered that triggered a rollback
	 * @param transaction the transaction status right before the rollback
	 */
	public MethodRollbackEvent(MethodInvocation invocation, Throwable failure, TransactionExecution transaction) {
		super(invocation, failure);
		this.transaction = transaction;
	}


	/**
	 * Return the exception encountered.
	 * <p>This may be an exception thrown by the method or emitted by the
	 * reactive publisher returned from the method.
	 */
	@Override
	public Throwable getFailure() {
		return super.getFailure();
	}

	/**
	 * Return the corresponding transaction status.
	 */
	public TransactionExecution getTransaction() {
		return this.transaction;
	}

}
