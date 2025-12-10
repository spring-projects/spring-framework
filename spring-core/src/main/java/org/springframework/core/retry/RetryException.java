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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Exception thrown when a {@link RetryPolicy} has been exhausted or interrupted.
 *
 * <p>A {@code RetryException} will typically contain the last exception thrown
 * by the {@link Retryable} operation as the {@linkplain #getCause() cause} and
 * any exceptions from previous attempts as {@linkplain #getSuppressed() suppressed
 * exceptions}.
 *
 * <p>Implements the {@link RetryState} interface for exposing the final outcome,
 * as a parameter of the terminal listener methods on {@link RetryListener}.
 *
 * @author Mahmoud Ben Hassine
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 7.0
 * @see RetryOperations
 */
public class RetryException extends Exception implements RetryState {

	@Serial
	private static final long serialVersionUID = 1L;


	/**
	 * Create a new {@code RetryException} for the supplied message and cause.
	 * @param message the detail message
	 * @param cause the last exception thrown by the {@link Retryable} operation
	 */
	public RetryException(String message, Throwable cause) {
		super(message, Objects.requireNonNull(cause, "cause must not be null"));
	}

	/**
	 * Create a new {@code RetryException} for the supplied message and state.
	 * @param message the detail message
	 * @param retryState the final retry state
	 * @since 7.0.2
	 */
	RetryException(String message, RetryState retryState) {
		super(message, retryState.getLastException());
		List<Throwable> exceptions = retryState.getExceptions();
		for (int i = 0; i < exceptions.size() - 1; i++) {
			addSuppressed(exceptions.get(i));
		}
	}


	/**
	 * Get the last exception thrown by the {@link Retryable} operation.
	 */
	@Override
	public final Throwable getCause() {
		return Objects.requireNonNull(super.getCause());
	}

	/**
	 * Return the number of retry attempts, or 0 if no retry has been attempted
	 * after the initial invocation at all.
	 */
	@Override
	public int getRetryCount() {
		return getSuppressed().length;
	}

	/**
	 * Return all invocation exceptions encountered, in the order of occurrence.
	 * @since 7.0.2
	 */
	@Override
	public List<Throwable> getExceptions() {
		Throwable[] suppressed = getSuppressed();
		List<Throwable> exceptions = new ArrayList<>(suppressed.length + 1);
		Collections.addAll(exceptions, suppressed);
		exceptions.add(getCause());
		return Collections.unmodifiableList(exceptions);
	}

	/**
	 * Return the exception from the last invocation (also exposed as the
	 * {@linkplain #getCause() cause}).
	 * @since 7.0.2
	 */
	@Override
	public Throwable getLastException() {
		return getCause();
	}

}
