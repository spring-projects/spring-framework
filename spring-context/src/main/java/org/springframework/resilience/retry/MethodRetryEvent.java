/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.resilience.retry;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.context.event.MethodFailureEvent;
import org.springframework.util.ClassUtils;

/**
 * Event published for every exception encountered during retryable method invocation.
 * Can be listened to via an {@code ApplicationListener<MethodRetryEvent>} bean or an
 * {@code @EventListener(MethodRetryEvent.class)} method.
 *
 * @author Juergen Hoeller
 * @since 7.0.3
 * @see AbstractRetryInterceptor
 * @see org.springframework.resilience.annotation.Retryable
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.event.EventListener
 */
@SuppressWarnings("serial")
public class MethodRetryEvent extends MethodFailureEvent {

	private final boolean retryAborted;


	/**
	 * Create a new event for the given retryable method invocation.
	 * @param invocation the retryable method invocation
	 * @param failure the exception encountered
	 * @param retryAborted whether the current failure led to the retry execution getting aborted
	 */
	public MethodRetryEvent(MethodInvocation invocation, Throwable failure, boolean retryAborted) {
		super(invocation, failure);
		this.retryAborted = retryAborted;
	}


	/**
	 * Return the exception encountered.
	 * <p>This may be an exception thrown by the method or emitted by the reactive
	 * publisher returned from the method, or a terminal exception on retry
	 * exhaustion, interruption or timeout.
	 * <p>For {@link org.springframework.core.retry.RetryTemplate} executions,
	 * an {@code instanceof RetryException} check identifies a final exception.
	 * For Reactor pipelines, {@code Exceptions.isRetryExhausted} identifies an
	 * exhaustion exception, whereas {@code instanceof TimeoutException} reveals
	 * a timeout scenario.
	 * @see #isRetryAborted()
	 * @see org.springframework.core.retry.RetryException
	 * @see reactor.core.Exceptions#isRetryExhausted
	 * @see java.util.concurrent.TimeoutException
	 */
	@Override
	public Throwable getFailure() {
		return super.getFailure();
	}

	/**
	 * Return whether the current failure led to the retry execution getting aborted,
	 * typically indicating exhaustion, interruption or a timeout scenario.
	 * <p>If this returns {@code true}, {@link #getFailure()} exposes the final exception
	 * thrown by the retry infrastructure (rather than thrown by the method itself).
	 * @see #getFailure()
	 */
	public boolean isRetryAborted() {
		return this.retryAborted;
	}


	@Override
	public String toString() {
		return "MethodRetryEvent: " + ClassUtils.getQualifiedMethodName(getMethod()) + " [" + getFailure() + "]";
	}

}
