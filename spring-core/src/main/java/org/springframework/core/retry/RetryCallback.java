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
 * Callback interface for a retryable block of code.
 *
 * <p>Used in conjunction with {@link RetryOperations}.
 *
 * @author Mahmoud Ben Hassine
 * @since 7.0
 * @param <R> the type of the result
 * @see RetryOperations
 */
@FunctionalInterface
public interface RetryCallback<R> {

	/**
	 * Method to execute and retry if needed.
	 * @return the result of the callback
	 * @throws Throwable if an error occurs during the execution of the callback
	 */
	R run() throws Throwable;

	/**
	 * A unique, logical name for this callback, used to distinguish retries for
	 * different business operations.
	 * <p>Defaults to the fully-qualified class name.
	 * @return the name of the callback
	 */
	default String getName() {
		return getClass().getName();
	}

}
