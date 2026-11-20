/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.retry;

import org.jspecify.annotations.Nullable;

/**
 * {@code Retryable} is a functional interface that can be used to implement any
 * generic block of code that can potentially be retried.
 *
 * <p>Used in conjunction with {@link RetryOperations}.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @since 7.0
 * @param <R> the type of the result
 * @see RetryOperations
 */
@FunctionalInterface
public interface Retryable<R extends @Nullable Object> {

	/**
	 * Method to execute and retry if needed.
	 * @return the result of the operation
	 * @throws Throwable if an error occurs during the execution of the operation
	 */
	R execute() throws Throwable;

	/**
	 * A unique, logical name for this retryable operation, used to distinguish
	 * between retries for different business operations.
	 * <p>Defaults to the fully-qualified class name of the implementation class.
	 * @return the name of this retryable operation
	 */
	default String getName() {
		return getClass().getName();
	}

}
