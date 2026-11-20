/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.retry;

import java.util.List;

/**
 * A representation of the current retry state, including the
 * current retry count and the exceptions accumulated so far.
 *
 * <p>Used as a parameter for {@link RetryListener#onRetryableExecution}.
 * Implemented by {@link RetryException} as well, exposing the final outcome in
 * the terminal listener methods {@link RetryListener#onRetryPolicyExhaustion},
 * {@link RetryListener#onRetryPolicyInterruption} and
 * {@link RetryListener#onRetryPolicyTimeout}.
 *
 * @author Juergen Hoeller
 * @since 7.0.2
 */
public interface RetryState {

	/**
	 * Return the current retry count: 0 indicates the initial invocation,
	 * 1 the first retry attempt, etc.
	 * <p>This may indicate the current attempt or the final number of
	 * retry attempts, depending on the time of the method call.
	 */
	int getRetryCount();

	/**
	 * Return the invocation exceptions accumulated so far,
	 * in the order of occurrence.
	 */
	List<Throwable> getExceptions();

	/**
	 * Return the recorded exception from the last invocation.
	 * @throws IllegalStateException if no exception has been recorded
	 */
	default Throwable getLastException() {
		List<Throwable> exceptions = getExceptions();
		if (exceptions.isEmpty()) {
			throw new IllegalStateException("No exception recorded");
		}
		return exceptions.get(exceptions.size() - 1);
	}

	/**
	 * Indicate whether a successful invocation has been accomplished.
	 */
	default boolean isSuccessful() {
		return getRetryCount() >= getExceptions().size();
	}

}
