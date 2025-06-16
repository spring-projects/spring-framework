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

/**
 * Strategy interface to define a retry execution created for a given
 * {@link RetryPolicy}.
 *
 * <p>A {@code RetryExecution} is effectively an executable instance of a given
 * {@code RetryPolicy}.
 *
 * <p>Implementations may be stateful but do not need to be thread-safe.
 *
 * @author Mahmoud Ben Hassine
 * @since 7.0
 */
public interface RetryExecution {

	/**
	 * Specify if the operation should be retried based on the given throwable.
	 * @param throwable the exception that caused the operation to fail
	 * @return {@code true} if the operation should be retried, {@code false} otherwise
	 */
	boolean shouldRetry(Throwable throwable);

}
