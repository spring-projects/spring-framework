/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util.concurrent;

import java.util.LinkedList;
import java.util.Queue;

import org.springframework.util.Assert;

/**
 * Helper class for {@link ListenableFuture} implementations that maintains a
 * of success and failure callbacks and helps to notify them.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.ExecutionList}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ListenableFutureCallbackRegistry<T> {

	private final Queue<SuccessCallback<? super T>> successCallbacks = new LinkedList<SuccessCallback<? super T>>();

	private final Queue<FailureCallback> failureCallbacks = new LinkedList<FailureCallback>();

	private State state = State.NEW;

	private Object result = null;

	private final Object mutex = new Object();


	/**
	 * Add the given callback to this registry.
	 * @param callback the callback to add
	 */
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		Assert.notNull(callback, "'callback' must not be null");
		synchronized (this.mutex) {
			switch (this.state) {
				case NEW:
					this.successCallbacks.add(callback);
					this.failureCallbacks.add(callback);
					break;
				case SUCCESS:
					notifySuccess(callback);
					break;
				case FAILURE:
					notifyFailure(callback);
					break;
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
	@SuppressWarnings("unchecked")
	public void addSuccessCallback(SuccessCallback<? super T> callback) {
		Assert.notNull(callback, "'callback' must not be null");
		synchronized (this.mutex) {
			switch (this.state) {
				case NEW:
					this.successCallbacks.add(callback);
					break;
				case SUCCESS:
					notifySuccess(callback);
					break;
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
				case NEW:
					this.failureCallbacks.add(callback);
					break;
				case FAILURE:
					notifyFailure(callback);
					break;
			}
		}
	}

	/**
	 * Trigger a {@link ListenableFutureCallback#onSuccess(Object)} call on all
	 * added callbacks with the given result.
	 * @param result the result to trigger the callbacks with
	 */
	public void success(T result) {
		synchronized (this.mutex) {
			this.state = State.SUCCESS;
			this.result = result;
			while (!this.successCallbacks.isEmpty()) {
				notifySuccess(this.successCallbacks.poll());
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
			while (!this.failureCallbacks.isEmpty()) {
				notifyFailure(this.failureCallbacks.poll());
			}
		}
	}


	private enum State {NEW, SUCCESS, FAILURE}

}
