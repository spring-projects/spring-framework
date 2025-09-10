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

package org.springframework.core.retry;

import java.io.Serial;
import java.util.Objects;

/**
 * Exception thrown when a {@link RetryPolicy} has been exhausted or interrupted.
 *
 * <p>A {@code RetryException} will typically contain the last exception thrown
 * by the {@link Retryable} operation as the {@linkplain #getCause() cause} and
 * any exceptions from previous attempts as {@linkplain #getSuppressed() suppressed
 * exceptions}.
 *
 * <p>However, if an {@link InterruptedException} is encountered while
 * {@linkplain Thread#sleep(long) sleeping} for the current
 * {@link org.springframework.util.backoff.BackOff BackOff} duration, a
 * {@code RetryException} will contain the {@code InterruptedException} as the
 * {@linkplain #getCause() cause} and any exceptions from previous invocations
 * of the {@code Retryable} operation as {@linkplain #getSuppressed() suppressed
 * exceptions}.
 *
 * @author Mahmoud Ben Hassine
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 7.0
 * @see RetryOperations
 */
public class RetryException extends Exception {

	@Serial
	private static final long serialVersionUID = 1L;


	/**
	 * Create a new {@code RetryException} for the supplied message and cause.
	 * @param message the detail message
	 * @param cause the last exception thrown by the {@link Retryable} operation,
	 * or an {@link InterruptedException} thrown while sleeping for the current
	 * {@code BackOff} duration
	 */
	public RetryException(String message, Throwable cause) {
		super(message, Objects.requireNonNull(cause, "cause must not be null"));
	}


	/**
	 * Get the last exception thrown by the {@link Retryable} operation, or an
	 * {@link InterruptedException} thrown while sleeping for the current
	 * {@code BackOff} duration.
	 */
	@Override
	public final Throwable getCause() {
		return Objects.requireNonNull(super.getCause());
	}

	/**
	 * Return the number of retry attempts, or 0 if no retry has been attempted
	 * after the initial invocation at all.
	 */
	public int getRetryCount() {
		return getSuppressed().length;
	}

}
