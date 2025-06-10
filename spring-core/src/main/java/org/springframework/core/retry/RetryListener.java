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

import org.springframework.core.retry.support.CompositeRetryListener;

/**
 * An extension point that allows to inject code during key retry phases.
 *
 * <p>Typically registered in a {@link RetryTemplate}, and can be composed using
 * a {@link CompositeRetryListener}.
 *
 * @author Mahmoud Ben Hassine
 * @since 7.0
 * @see CompositeRetryListener
 */
public interface RetryListener {

	/**
	 * Called before every retry attempt.
	 * @param retryExecution the retry execution
	 */
	default void beforeRetry(RetryExecution retryExecution) {
	}

	/**
	 * Called after the first successful retry attempt.
	 * @param retryExecution the retry execution
	 * @param result the result of the {@link Retryable}
	 */
	default void onRetrySuccess(RetryExecution retryExecution, Object result) {
	}

	/**
	 * Called every time a retry attempt fails.
	 * @param retryExecution the retry execution
	 * @param throwable the exception thrown by the {@link Retryable}
	 */
	default void onRetryFailure(RetryExecution retryExecution, Throwable throwable) {
	}

	/**
	 * Called if the {@link RetryPolicy} is exhausted.
	 * @param retryExecution the retry execution
	 * @param throwable the last exception thrown by the {@link Retryable}
	 */
	default void onRetryPolicyExhaustion(RetryExecution retryExecution, Throwable throwable) {
	}

}
