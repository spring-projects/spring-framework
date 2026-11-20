/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
