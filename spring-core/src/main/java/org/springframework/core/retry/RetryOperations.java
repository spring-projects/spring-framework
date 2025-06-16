/*
 * Copyright 2002-2025 the original author or authors.
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
 * @author Mahmoud Ben Hassine
 * @since 7.0
 * @see RetryTemplate
 */
public interface RetryOperations {

	/**
	 * Execute the given {@link Retryable} (according to the {@link RetryPolicy}
	 * configured at the implementation level) until it succeeds, or eventually
	 * throw an exception if the {@code RetryPolicy} is exhausted.
	 * @param retryable the {@code Retryable} to execute and retry if needed
	 * @param <R> the type of the result
	 * @return the result of the {@code Retryable}, if any
	 * @throws RetryException if the {@code RetryPolicy} is exhausted; exceptions
	 * encountered during retry attempts should be made available as suppressed
	 * exceptions
	 */
	<R> @Nullable R execute(Retryable<? extends @Nullable R> retryable) throws RetryException;

}
