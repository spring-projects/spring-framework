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

import java.util.LinkedList;
import java.util.List;

import org.springframework.core.retry.RetryExecution;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.Assert;

/**
 * A composite implementation of the {@link RetryListener} interface.
 *
 * <p>This class is used to compose multiple listeners within a {@link RetryTemplate}.
 *
 * <p>Delegate listeners will be called in their registration order.
 *
 * @author Mahmoud Ben Hassine
 * @since 7.0
 */
public class CompositeRetryListener implements RetryListener {

	private final List<RetryListener> listeners = new LinkedList<>();


	/**
	 * Create a new {@code CompositeRetryListener}.
	 */
	public CompositeRetryListener() {
	}

	/**
	 * Create a new {@code CompositeRetryListener} with the supplied list of
	 * delegates.
	 * @param listeners the list of delegate listeners to register; must not be empty
	 */
	public CompositeRetryListener(List<RetryListener> listeners) {
		Assert.notEmpty(listeners, "RetryListener List must not be empty");
		this.listeners.addAll(listeners);
	}

	/**
	 * Add a new listener to the list of delegates.
	 * @param listener the listener to add
	 */
	public void addListener(RetryListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void beforeRetry(RetryExecution retryExecution) {
		this.listeners.forEach(retryListener -> retryListener.beforeRetry(retryExecution));
	}

	@Override
	public void onRetrySuccess(RetryExecution retryExecution, Object result) {
		this.listeners.forEach(listener -> listener.onRetrySuccess(retryExecution, result));
	}

	@Override
	public void onRetryFailure(RetryExecution retryExecution, Throwable throwable) {
		this.listeners.forEach(listener -> listener.onRetryFailure(retryExecution, throwable));
	}

	@Override
	public void onRetryPolicyExhaustion(RetryExecution retryExecution, Throwable throwable) {
		this.listeners.forEach(listener -> listener.onRetryPolicyExhaustion(retryExecution, throwable));
	}

}
