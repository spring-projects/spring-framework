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
public interface Retryable<R> {

	/**
	 * Method to execute and retry if needed.
	 * @return the result of the operation
	 * @throws Throwable if an error occurs during the execution of the operation
	 */
	@Nullable R execute() throws Throwable;

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
