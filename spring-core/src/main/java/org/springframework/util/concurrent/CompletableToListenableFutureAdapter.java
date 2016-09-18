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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * Adapts a {@link CompletableFuture} into a {@link ListenableFuture}.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public class CompletableToListenableFutureAdapter<T> implements ListenableFuture<T> {

	private final CompletableFuture<T> completableFuture;

	private final ListenableFutureCallbackRegistry<T> callbacks = new ListenableFutureCallbackRegistry<>();


	public CompletableToListenableFutureAdapter(CompletableFuture<T> completableFuture) {
		this.completableFuture = completableFuture;
		this.completableFuture.whenComplete(new BiConsumer<T, Throwable>() {
			@Override
			public void accept(T result, Throwable ex) {
				if (ex != null) {
					callbacks.failure(ex);
				}
				else {
					callbacks.success(result);
				}
			}
		});
	}


	@Override
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		this.callbacks.addCallback(callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
		this.callbacks.addSuccessCallback(successCallback);
		this.callbacks.addFailureCallback(failureCallback);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return this.completableFuture.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return this.completableFuture.isCancelled();
	}

	@Override
	public boolean isDone() {
		return this.completableFuture.isDone();
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		return this.completableFuture.get();
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return this.completableFuture.get(timeout, unit);
	}

}
