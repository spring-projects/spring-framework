/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.retry.support;

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
	void onRetryableExecution() {
		RetryException exception = new RetryException("", new Exception());
		compositeRetryListener.onRetryableExecution(retryPolicy, retryable, exception);

		verify(listener1).onRetryableExecution(retryPolicy, retryable, exception);
		verify(listener2).onRetryableExecution(retryPolicy, retryable, exception);
		verify(listener3).onRetryableExecution(retryPolicy, retryable, exception);
	}

	@Test
	void beforeRetry() {
		RetryException exception = new RetryException("", new Exception());
		compositeRetryListener.beforeRetry(retryPolicy, retryable, exception);

		verify(listener1).beforeRetry(retryPolicy, retryable, exception);
		verify(listener2).beforeRetry(retryPolicy, retryable, exception);
		verify(listener3).beforeRetry(retryPolicy, retryable, exception);
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
		RetryException exception = new RetryException("", new Exception());
		compositeRetryListener.onRetryPolicyTimeout(retryPolicy, retryable, exception);

		verify(listener1).onRetryPolicyTimeout(retryPolicy, retryable, exception);
		verify(listener2).onRetryPolicyTimeout(retryPolicy, retryable, exception);
		verify(listener3).onRetryPolicyTimeout(retryPolicy, retryable, exception);
	}

}
