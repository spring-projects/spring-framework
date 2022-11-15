/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.util.concurrent;

import java.util.ArrayDeque;
import java.util.Queue;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Helper class for {@link ListenableFuture} implementations that maintains a queue
 * of success and failure callbacks and helps to notify them.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.ExecutionList}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <T> the callback result type
 * @deprecated as of 6.0, with no concrete replacement
 */
@Deprecated(since = "6.0")
public class ListenableFutureCallbackRegistry<T> {

	private final Queue<SuccessCallback<? super T>> successCallbacks = new ArrayDeque<>(1);

	private final Queue<FailureCallback> failureCallbacks = new ArrayDeque<>(1);

	private State state = State.NEW;

	@Nullable
	private Object result;

	private final Object mutex = new Object();


	/**
	 * Add the given callback to this registry.
	 * @param callback the callback to add
	 */
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		Assert.notNull(callback, "'callback' must not be null");
		synchronized (this.mutex) {
			switch (this.state) {
				case NEW -> {
					this.successCallbacks.add(callback);
					this.failureCallbacks.add(callback);
				}
				case SUCCESS -> notifySuccess(callback);
				case FAILURE -> notifyFailure(callback);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void notifySuccess(SuccessCallback<? super T> callback) {
		try {
			callback.onSuccess((T) this.result);
		}
		catch (Throwable ex) {
			// Ignore
		}
	}

	private void notifyFailure(FailureCallback callback) {
		Assert.state(this.result instanceof Throwable, "No Throwable result for failure state");
		try {
			callback.onFailure((Throwable) this.result);
		}
		catch (Throwable ex) {
			// Ignore
		}
	}

	/**
	 * Add the given success callback to this registry.
	 * @param callback the success callback to add
	 * @since 4.1
	 */
	public void addSuccessCallback(SuccessCallback<? super T> callback) {
		Assert.notNull(callback, "'callback' must not be null");
		synchronized (this.mutex) {
			switch (this.state) {
				case NEW -> this.successCallbacks.add(callback);
				case SUCCESS -> notifySuccess(callback);
			}
		}
	}

	/**
	 * Add the given failure callback to this registry.
	 * @param callback the failure callback to add
	 * @since 4.1
	 */
	public void addFailureCallback(FailureCallback callback) {
		Assert.notNull(callback, "'callback' must not be null");
		synchronized (this.mutex) {
			switch (this.state) {
				case NEW -> this.failureCallbacks.add(callback);
				case FAILURE -> notifyFailure(callback);
			}
		}
	}

	/**
	 * Trigger a {@link ListenableFutureCallback#onSuccess(Object)} call on all
	 * added callbacks with the given result.
	 * @param result the result to trigger the callbacks with
	 */
	public void success(@Nullable T result) {
		synchronized (this.mutex) {
			this.state = State.SUCCESS;
			this.result = result;
			SuccessCallback<? super T> callback;
			while ((callback = this.successCallbacks.poll()) != null) {
				notifySuccess(callback);
			}
		}
	}

	/**
	 * Trigger a {@link ListenableFutureCallback#onFailure(Throwable)} call on all
	 * added callbacks with the given {@code Throwable}.
	 * @param ex the exception to trigger the callbacks with
	 */
	public void failure(Throwable ex) {
		synchronized (this.mutex) {
			this.state = State.FAILURE;
			this.result = ex;
			FailureCallback callback;
			while ((callback = this.failureCallbacks.poll()) != null) {
				notifyFailure(callback);
			}
		}
	}


	private enum State {NEW, SUCCESS, FAILURE}

}
