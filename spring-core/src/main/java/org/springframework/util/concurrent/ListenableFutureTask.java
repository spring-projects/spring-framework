/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Extension of {@link FutureTask} that implements {@link ListenableFuture}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public class ListenableFutureTask<T> extends FutureTask<T> implements ListenableFuture<T> {

	private final ListenableFutureCallbackRegistry<T> callbacks = new ListenableFutureCallbackRegistry<T>();


	/**
	 * Create a new {@code ListenableFutureTask} that will, upon running,
	 * execute the given {@link Callable}.
	 * @param callable the callable task
	 */
	public ListenableFutureTask(Callable<T> callable) {
		super(callable);
	}

	/**
	 * Create a {@code ListenableFutureTask} that will, upon running,
	 * execute the given {@link Runnable}, and arrange that {@link #get()}
	 * will return the given result on successful completion.
	 * @param runnable the runnable task
	 * @param result the result to return on successful completion
	 */
	public ListenableFutureTask(Runnable runnable, T result) {
		super(runnable, result);
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
	protected void done() {
		Throwable cause;
		try {
			T result = get();
			this.callbacks.success(result);
			return;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return;
		}
		catch (ExecutionException ex) {
			cause = ex.getCause();
			if (cause == null) {
				cause = ex;
			}
		}
		catch (Throwable ex) {
			cause = ex;
		}
		this.callbacks.failure(cause);
	}

}
