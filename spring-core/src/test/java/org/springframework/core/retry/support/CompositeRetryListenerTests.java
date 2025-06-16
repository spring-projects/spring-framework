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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.retry.RetryExecution;
import org.springframework.core.retry.RetryListener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CompositeRetryListener}.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @since 7.0
 */
class CompositeRetryListenerTests {

	private final RetryListener listener1 = mock();
	private final RetryListener listener2 = mock();
	private final RetryListener listener3 = mock();
	private final RetryExecution retryExecution = mock();

	private final CompositeRetryListener compositeRetryListener =
			new CompositeRetryListener(List.of(listener1, listener2));


	@BeforeEach
	void addListener() {
		compositeRetryListener.addListener(listener3);
	}

	@Test
	void beforeRetry() {
		compositeRetryListener.beforeRetry(retryExecution);

		verify(listener1).beforeRetry(retryExecution);
		verify(listener2).beforeRetry(retryExecution);
		verify(listener3).beforeRetry(retryExecution);
	}

	@Test
	void onRetrySuccess() {
		Object result = new Object();
		compositeRetryListener.onRetrySuccess(retryExecution, result);

		verify(listener1).onRetrySuccess(retryExecution, result);
		verify(listener2).onRetrySuccess(retryExecution, result);
		verify(listener3).onRetrySuccess(retryExecution, result);
	}

	@Test
	void onRetryFailure() {
		Exception exception = new Exception();
		compositeRetryListener.onRetryFailure(retryExecution, exception);

		verify(listener1).onRetryFailure(retryExecution, exception);
		verify(listener2).onRetryFailure(retryExecution, exception);
		verify(listener3).onRetryFailure(retryExecution, exception);
	}

	@Test
	void onRetryPolicyExhaustion() {
		Exception exception = new Exception();
		compositeRetryListener.onRetryPolicyExhaustion(retryExecution, exception);

		verify(listener1).onRetryPolicyExhaustion(retryExecution, exception);
		verify(listener2).onRetryPolicyExhaustion(retryExecution, exception);
		verify(listener3).onRetryPolicyExhaustion(retryExecution, exception);
	}

}
