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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.util.Assert;

/**
 * A {@link ListenableFuture} whose value can be set via {@link #set(Object)}
 * or {@link #setException(Throwable)}. It may also get cancelled.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.SettableFuture}.
 *
 * @author Mattias Severson
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.1
 */
public class SettableListenableFuture<T> implements ListenableFuture<T> {

	private static final Callable<Object> DUMMY_CALLABLE = new Callable<Object>() {
		@Override
		public Object call() throws Exception {
			throw new IllegalStateException("Should never be called");
		}
	};


	private final SettableTask<T> settableTask = new SettableTask<T>();


	/**
	 * Set the value of this future. This method will return {@code true} if the
	 * value was set successfully, or {@code false} if the future has already been
	 * set or cancelled.
	 * @param value the value that will be set
	 * @return {@code true} if the value was successfully set, else {@code false}
	 */
	public boolean set(T value) {
		return this.settableTask.setResultValue(value);
	}

	/**
	 * Set the exception of this future. This method will return {@code true} if the
	 * exception was set successfully, or {@code false} if the future has already been
	 * set or cancelled.
	 * @param exception the value that will be set
	 * @return {@code true} if the exception was successfully set, else {@code false}
	 */
	public boolean setException(Throwable exception) {
		Assert.notNull(exception, "Exception must not be null");
		return this.settableTask.setExceptionResult(exception);
	}

	@Override
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		this.settableTask.addCallback(callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
		this.settableTask.addCallback(successCallback, failureCallback);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		boolean cancelled = this.settableTask.cancel(mayInterruptIfRunning);
		if (cancelled && mayInterruptIfRunning) {
			interruptTask();
		}
		return cancelled;
	}

	@Override
	public boolean isCancelled() {
		return this.settableTask.isCancelled();
	}

	@Override
	public boolean isDone() {
		return this.settableTask.isDone();
	}

	/**
	 * Retrieve the value.
	 * <p>This method returns the value if it has been set via {@link #set(Object)},
	 * throws an {@link java.util.concurrent.ExecutionException} if an exception has
	 * been set via {@link #setException(Throwable)}, or throws a
	 * {@link java.util.concurrent.CancellationException} if the future has been cancelled.
	 * @return the value associated with this future
	 */
	@Override
	public T get() throws InterruptedException, ExecutionException {
		return this.settableTask.get();
	}

	/**
	 * Retrieve the value.
	 * <p>This method returns the value if it has been set via {@link #set(Object)},
	 * throws an {@link java.util.concurrent.ExecutionException} if an exception has
	 * been set via {@link #setException(Throwable)}, or throws a
	 * {@link java.util.concurrent.CancellationException} if the future has been cancelled.
	 * @param timeout the maximum time to wait
	 * @param unit the unit of the timeout argument
	 * @return the value associated with this future
	 */
	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return this.settableTask.get(timeout, unit);
	}

	/**
	 * Subclasses can override this method to implement interruption of the future's
	 * computation. The method is invoked automatically by a successful call to
	 * {@link #cancel(boolean) cancel(true)}.
	 * <p>The default implementation is empty.
	 */
	protected void interruptTask() {
	}


	private static class SettableTask<T> extends ListenableFutureTask<T> {

		private volatile Thread completingThread;

		@SuppressWarnings("unchecked")
		public SettableTask() {
			super((Callable<T>) DUMMY_CALLABLE);
		}

		public boolean setResultValue(T value) {
			set(value);
			return checkCompletingThread();
		}

		public boolean setExceptionResult(Throwable exception) {
			setException(exception);
			return checkCompletingThread();
		}

		@Override
		protected void done() {
			if (!isCancelled()) {
				// Implicitly invoked by set/setException: store current thread for
				// determining whether the given result has actually triggered completion
				// (since FutureTask.set/setException unfortunately don't expose that)
				this.completingThread = Thread.currentThread();
			}
			super.done();
		}

		private boolean checkCompletingThread() {
			boolean check = (this.completingThread == Thread.currentThread());
			if (check) {
				this.completingThread = null;  // only first match actually counts
			}
			return check;
		}
	}

}
