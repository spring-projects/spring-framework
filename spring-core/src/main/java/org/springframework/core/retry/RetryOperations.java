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

import org.jspecify.annotations.Nullable;

/**
 * Interface specifying basic retry operations.
 *
 * <p>Implemented by {@link RetryTemplate}. Not often used directly, but a useful
 * option to enhance testability, as it can easily be mocked or stubbed.
 *
 * <p>Inspired by the <a href="https://github.com/spring-projects/spring-retry">Spring Retry</a>
 * project but redesigned as a minimal core retry feature in the Spring Framework.
 *
 * @author Mahmoud Ben Hassine
 * @since 7.0
 * @see RetryTemplate
 */
public interface RetryOperations {

	/**
	 * Execute the given {@link Retryable} operation according to the {@link RetryPolicy}
	 * configured at the implementation level.
	 * <p>If the {@code Retryable} succeeds, its result will be returned. Otherwise, a
	 * {@link RetryException} will be thrown to the caller. The {@code RetryException}
	 * will contain the last exception thrown by the {@code Retryable} operation as the
	 * {@linkplain RetryException#getCause() cause} and any exceptions from previous
	 * attempts as {@linkplain RetryException#getSuppressed() suppressed exceptions}.
	 * @param retryable the {@code Retryable} to execute and retry if needed
	 * @param <R> the type of the result
	 * @return the result of the {@code Retryable}, if any
	 * @throws RetryException if the {@code RetryPolicy} is exhausted
	 */
	<R> @Nullable R execute(Retryable<? extends @Nullable R> retryable) throws RetryException;

}
