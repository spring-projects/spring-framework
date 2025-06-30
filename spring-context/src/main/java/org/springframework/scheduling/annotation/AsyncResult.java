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

package org.springframework.scheduling.annotation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

/**
 * A pass-through {@code Future} handle that can be used for method signatures
 * which are declared with a {@code Future} return type for asynchronous execution.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @param <V> the value type
 * @see Async
 * @see #forValue(Object)
 * @see #forExecutionException(Throwable)
 * @deprecated as of 6.0, in favor of {@link CompletableFuture}
 */
@Deprecated(since = "6.0")
public class AsyncResult<V> implements Future<V> {

	private final @Nullable V value;

	private final @Nullable Throwable executionException;


	/**
	 * Create a new AsyncResult holder.
	 * @param value the value to pass through
	 */
	public AsyncResult(@Nullable V value) {
		this(value, null);
	}

	/**
	 * Create a new AsyncResult holder.
	 * @param value the value to pass through
	 */
	private AsyncResult(@Nullable V value, @Nullable Throwable ex) {
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
	public @Nullable V get() throws ExecutionException {
		if (this.executionException != null) {
			throw (this.executionException instanceof ExecutionException execEx ? execEx :
					new ExecutionException(this.executionException));
		}
		return this.value;
	}

	@Override
	public @Nullable V get(long timeout, TimeUnit unit) throws ExecutionException {
		return get();
	}


	/**
	 * Create a new async result which exposes the given value from {@link Future#get()}.
	 * @param value the value to expose
	 * @since 4.2
	 * @see Future#get()
	 */
	public static <V> Future<V> forValue(V value) {
		return new AsyncResult<>(value, null);
	}

	/**
	 * Create a new async result which exposes the given exception as an
	 * {@link ExecutionException} from {@link Future#get()}.
	 * @param ex the exception to expose (either an pre-built {@link ExecutionException}
	 * or a cause to be wrapped in an {@link ExecutionException})
	 * @since 4.2
	 * @see ExecutionException
	 */
	public static <V> Future<V> forExecutionException(Throwable ex) {
		return new AsyncResult<>(null, ex);
	}

}
