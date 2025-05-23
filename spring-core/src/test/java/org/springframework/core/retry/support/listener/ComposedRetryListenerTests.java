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

package org.springframework.core.retry.support.listener;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.core.retry.RetryListener;

import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CompositeRetryListener}.
 *
 * @author Mahmoud Ben Hassine
 */
class ComposedRetryListenerTests {

	private final RetryListener listener1 = Mockito.mock(RetryListener.class);
	private final RetryListener listener2 = Mockito.mock(RetryListener.class);

	private final CompositeRetryListener composedRetryListener = new CompositeRetryListener(Arrays.asList(listener1, listener2));

	@Test
	void beforeRetry() {
		this.composedRetryListener.beforeRetry();

		verify(this.listener1).beforeRetry();
		verify(this.listener2).beforeRetry();
	}

	@Test
	void onSuccess() {
		Object result = new Object();
		this.composedRetryListener.onSuccess(result);

		verify(this.listener1).onSuccess(result);
		verify(this.listener2).onSuccess(result);
	}

	@Test
	void onFailure() {
		Exception exception = new Exception();
		this.composedRetryListener.onFailure(exception);

		verify(this.listener1).onFailure(exception);
		verify(this.listener2).onFailure(exception);
	}

	@Test
	void onMaxAttempts() {
		Exception exception = new Exception();
		this.composedRetryListener.onMaxAttempts(exception);

		verify(this.listener1).onMaxAttempts(exception);
		verify(this.listener2).onMaxAttempts(exception);
	}
}
