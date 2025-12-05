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

import java.util.LinkedList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.Retryable;
import org.springframework.util.Assert;

/**
 * A composite implementation of the {@link RetryListener} interface, which is
 * used to compose multiple listeners within a
 * {@link org.springframework.core.retry.RetryTemplate RetryTemplate}.
 *
 * <p>Delegate listeners will be called in their registration order.
 *
 * @author Mahmoud Ben Hassine
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 7.0
 */
public class CompositeRetryListener implements RetryListener {

	private final List<RetryListener> listeners = new LinkedList<>();


	/**
	 * Create a new {@code CompositeRetryListener}.
	 * @see #addListener(RetryListener)
	 */
	public CompositeRetryListener() {
	}

	/**
	 * Create a new {@code CompositeRetryListener} with the supplied list of
	 * delegates.
	 * @param listeners the list of delegate listeners to register; must not be empty
	 */
	public CompositeRetryListener(List<RetryListener> listeners) {
		Assert.notEmpty(listeners, "RetryListener list must not be empty");
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
	public void beforeRetry(RetryPolicy retryPolicy, Retryable<?> retryable) {
		this.listeners.forEach(retryListener -> retryListener.beforeRetry(retryPolicy, retryable));
	}

	@Override
	public void onRetrySuccess(RetryPolicy retryPolicy, Retryable<?> retryable, @Nullable Object result) {
		this.listeners.forEach(listener -> listener.onRetrySuccess(retryPolicy, retryable, result));
	}

	@Override
	public void onRetryFailure(RetryPolicy retryPolicy, Retryable<?> retryable, Throwable throwable) {
		this.listeners.forEach(listener -> listener.onRetryFailure(retryPolicy, retryable, throwable));
	}

	@Override
	public void onRetryPolicyExhaustion(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
		this.listeners.forEach(listener -> listener.onRetryPolicyExhaustion(retryPolicy, retryable, exception));
	}

	@Override
	public void onRetryPolicyInterruption(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
		this.listeners.forEach(listener -> listener.onRetryPolicyInterruption(retryPolicy, retryable, exception));
	}

	@Override
	public void onRetryPolicyTimeout(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
		this.listeners.forEach(listener -> listener.onRetryPolicyTimeout(retryPolicy, retryable, exception));
	}

}
