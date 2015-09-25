/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.messaging.tcp.reactor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.util.Assert;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureCallbackRegistry;
import org.springframework.util.concurrent.SuccessCallback;
import reactor.fn.Consumer;
import reactor.rx.Promise;

/**
 * Adapts a reactor {@link Promise} to {@link ListenableFuture} optionally converting
 * the result Object type {@code <S>} to the expected target type {@code <T>}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <S> the type of object expected from the {@link Promise}
 * @param <T> the type of object expected from the {@link ListenableFuture}
 */
abstract class AbstractPromiseToListenableFutureAdapter<S, T> implements ListenableFuture<T> {

	private final Promise<S> promise;

	private final ListenableFutureCallbackRegistry<T> registry = new ListenableFutureCallbackRegistry<T>();


	protected AbstractPromiseToListenableFutureAdapter(Promise<S> promise) {
		Assert.notNull(promise, "Promise must not be null");
		this.promise = promise;

		this.promise.onSuccess(new Consumer<S>() {
			@Override
			public void accept(S result) {
				try {
					registry.success(adapt(result));
				}
				catch (Throwable t) {
					registry.failure(t);
				}
			}
		});

		this.promise.onError(new Consumer<Throwable>() {
			@Override
			public void accept(Throwable t) {
				registry.failure(t);
			}
		});
	}

	protected abstract T adapt(S result);

	@Override
	public T get() throws InterruptedException {
		S result = this.promise.await();
		return adapt(result);
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		S result = this.promise.await(timeout, unit);
		if (!this.promise.isComplete()) {
			throw new TimeoutException();
		}
		return adapt(result);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return this.promise.isComplete();
	}

	@Override
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		this.registry.addCallback(callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
		this.registry.addSuccessCallback(successCallback);
		this.registry.addFailureCallback(failureCallback);
	}

}
