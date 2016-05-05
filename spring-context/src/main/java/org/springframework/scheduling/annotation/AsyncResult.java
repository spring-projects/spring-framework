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

package org.springframework.scheduling.annotation;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * A pass-through {@code Future} handle that can be used for method signatures
 * which are declared with a {@code Future} return type for asynchronous execution.
 *
 * <p>As of Spring 4.1, this class implements {@link ListenableFuture}, not just
 * plain {@link java.util.concurrent.Future}, along with the corresponding support
 * in {@code @Async} processing.
 *
 * <p>As of Spring 4.2, this class also supports passing execution exceptions back
 * to the caller.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @see Async
 * @see #forValue(Object)
 * @see #forExecutionException(Throwable)
 */
public class AsyncResult<V> implements ListenableFuture<V> {

	private final V value;

	private final ExecutionException executionException;


	/**
	 * Create a new AsyncResult holder.
	 * @param value the value to pass through
	 */
	public AsyncResult(V value) {
		this(value, null);
	}

	/**
	 * Create a new AsyncResult holder.
	 * @param value the value to pass through
	 */
	private AsyncResult(V value, ExecutionException ex) {
		this.value = value;
		this.executionException = ex;
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
		return true;
	}

	@Override
	public V get() throws ExecutionException {
		if (this.executionException != null) {
			throw this.executionException;
		}
		return this.value;
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws ExecutionException {
		return get();
	}

	@Override
	public void addCallback(ListenableFutureCallback<? super V> callback) {
		addCallback(callback, callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super V> successCallback, FailureCallback failureCallback) {
		if (this.executionException != null) {
			Throwable cause = this.executionException.getCause();
			failureCallback.onFailure(cause != null ? cause : this.executionException);
		}
		else {
			try {
				successCallback.onSuccess(this.value);
			}
			catch (Throwable ex) {
				failureCallback.onFailure(ex);
			}
		}
	}


	/**
	 * Create a new async result which exposes the given value from {@link Future#get()}.
	 * @param value the value to expose
	 * @since 4.2
	 * @see Future#get()
	 */
	public static <V> ListenableFuture<V> forValue(V value) {
		return new AsyncResult<V>(value, null);
	}

	/**
	 * Create a new async result which exposes the given exception as an
	 * {@link ExecutionException} from {@link Future#get()}.
	 * @param ex the exception to expose (either an pre-built {@link ExecutionException}
	 * or a cause to be wrapped in an {@link ExecutionException})
	 * @since 4.2
	 * @see ExecutionException
	 */
	public static <V> ListenableFuture<V> forExecutionException(Throwable ex) {
		return new AsyncResult<V>(null,
				(ex instanceof ExecutionException ? (ExecutionException) ex : new ExecutionException(ex)));
	}

}
