/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Adapts a {@link CompletableFuture} or {@link CompletionStage} into a
 * Spring {@link ListenableFuture}.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.2
 */
public class CompletableToListenableFutureAdapter<T> implements ListenableFuture<T> {

	private final CompletableFuture<T> completableFuture;

	private final ListenableFutureCallbackRegistry<T> callbacks = new ListenableFutureCallbackRegistry<>();


	/**
	 * Create a new adapter for the given {@link CompletionStage}.
	 * @since 4.3.7
	 */
	public CompletableToListenableFutureAdapter(CompletionStage<T> completionStage) {
		this(completionStage.toCompletableFuture());
	}

	/**
	 * Create a new adapter for the given {@link CompletableFuture}.
	 */
	public CompletableToListenableFutureAdapter(CompletableFuture<T> completableFuture) {
		this.completableFuture = completableFuture;
		this.completableFuture.handle((result, ex) -> {
			if (ex != null) {
				callbacks.failure(ex);
			}
			else {
				callbacks.success(result);
			}
			return null;
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
	public CompletableFuture<T> completable() {
		return this.completableFuture;
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
