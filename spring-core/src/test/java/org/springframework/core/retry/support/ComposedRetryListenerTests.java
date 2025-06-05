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

package org.springframework.core.retry.support;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.core.retry.RetryExecution;
import org.springframework.core.retry.RetryListener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CompositeRetryListener}.
 *
 * @author Mahmoud Ben Hassine
 */
class ComposedRetryListenerTests {

	private final RetryListener listener1 = mock();
	private final RetryListener listener2 = mock();

	private final CompositeRetryListener composedRetryListener = new CompositeRetryListener(Arrays.asList(listener1, listener2));

	@Test
	void beforeRetry() {
		RetryExecution retryExecution = mock();
		this.composedRetryListener.beforeRetry(retryExecution);

		verify(this.listener1).beforeRetry(retryExecution);
		verify(this.listener2).beforeRetry(retryExecution);
	}

	@Test
	void onSuccess() {
		Object result = new Object();
		RetryExecution retryExecution = mock();
		this.composedRetryListener.onRetrySuccess(retryExecution, result);

		verify(this.listener1).onRetrySuccess(retryExecution, result);
		verify(this.listener2).onRetrySuccess(retryExecution, result);
	}

	@Test
	void onFailure() {
		Exception exception = new Exception();
		RetryExecution retryExecution = mock();
		this.composedRetryListener.onRetryFailure(retryExecution, exception);

		verify(this.listener1).onRetryFailure(retryExecution, exception);
		verify(this.listener2).onRetryFailure(retryExecution, exception);
	}

	@Test
	void onMaxAttempts() {
		Exception exception = new Exception();
		RetryExecution retryExecution = mock();
		this.composedRetryListener.onRetryPolicyExhaustion(retryExecution, exception);

		verify(this.listener1).onRetryPolicyExhaustion(retryExecution, exception);
		verify(this.listener2).onRetryPolicyExhaustion(retryExecution, exception);
	}

}
