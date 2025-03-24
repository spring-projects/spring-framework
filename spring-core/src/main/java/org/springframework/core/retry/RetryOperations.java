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
 * Main entry point to the core retry functionality. Defines a set of retryable operations.
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
	 * Retry the given callback (according to the retry policy configured at the implementation level)
	 * until it succeeds or eventually throw an exception if the retry policy is exhausted.
	 * @param retryCallback the callback to call initially and retry if needed
	 * @param <R> the type of the callback's result
	 * @return the callback's result
	 * @throws RetryException thrown if the retry policy is exhausted. All attempt exceptions
	 * should be added as suppressed exceptions to the final exception.
	 */
	<R extends @Nullable Object> R execute(RetryCallback<R> retryCallback) throws RetryException;

}

