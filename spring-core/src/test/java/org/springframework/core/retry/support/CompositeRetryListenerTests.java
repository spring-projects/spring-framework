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

package org.springframework.core.retry.support;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.Retryable;

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
	private final RetryPolicy retryPolicy = mock();
	private final Retryable<?> retryable = mock();

	private final CompositeRetryListener compositeRetryListener =
			new CompositeRetryListener(List.of(listener1, listener2));


	@BeforeEach
	void addListener() {
		compositeRetryListener.addListener(listener3);
	}

	@Test
	void beforeRetry() {
		compositeRetryListener.beforeRetry(retryPolicy, retryable);

		verify(listener1).beforeRetry(retryPolicy, retryable);
		verify(listener2).beforeRetry(retryPolicy, retryable);
		verify(listener3).beforeRetry(retryPolicy, retryable);
	}

	@Test
	void onRetrySuccess() {
		Object result = new Object();
		compositeRetryListener.onRetrySuccess(retryPolicy, retryable, result);

		verify(listener1).onRetrySuccess(retryPolicy, retryable, result);
		verify(listener2).onRetrySuccess(retryPolicy, retryable, result);
		verify(listener3).onRetrySuccess(retryPolicy, retryable, result);
	}

	@Test
	void onRetryFailure() {
		Exception exception = new Exception();
		compositeRetryListener.onRetryFailure(retryPolicy, retryable, exception);

		verify(listener1).onRetryFailure(retryPolicy, retryable, exception);
		verify(listener2).onRetryFailure(retryPolicy, retryable, exception);
		verify(listener3).onRetryFailure(retryPolicy, retryable, exception);
	}

	@Test
	void onRetryPolicyExhaustion() {
		RetryException exception = new RetryException("", new Exception());
		compositeRetryListener.onRetryPolicyExhaustion(retryPolicy, retryable, exception);

		verify(listener1).onRetryPolicyExhaustion(retryPolicy, retryable, exception);
		verify(listener2).onRetryPolicyExhaustion(retryPolicy, retryable, exception);
		verify(listener3).onRetryPolicyExhaustion(retryPolicy, retryable, exception);
	}

	@Test
	void onRetryPolicyInterruption() {
		RetryException exception = new RetryException("", new Exception());
		compositeRetryListener.onRetryPolicyInterruption(retryPolicy, retryable, exception);

		verify(listener1).onRetryPolicyInterruption(retryPolicy, retryable, exception);
		verify(listener2).onRetryPolicyInterruption(retryPolicy, retryable, exception);
		verify(listener3).onRetryPolicyInterruption(retryPolicy, retryable, exception);
	}

	@Test
	void onRetryPolicyTimeout() {
		Duration elapsedTime = Duration.ofMillis(100);
		RetryException exception = new RetryException("", new Exception());
		compositeRetryListener.onRetryPolicyTimeout(retryPolicy, retryable, exception);

		verify(listener1).onRetryPolicyTimeout(retryPolicy, retryable, exception);
		verify(listener2).onRetryPolicyTimeout(retryPolicy, retryable, exception);
		verify(listener3).onRetryPolicyTimeout(retryPolicy, retryable, exception);
	}

}
