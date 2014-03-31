/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.util.Assert;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A {@link ListenableFuture} whose value can be set by the {@link #set(Object)} or
 * {@link #setException(Throwable)}. It may also be cancelled.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.SettableFuture}.
 *
 * @author Mattias Severson
 * @since 4.1
 */
public class SettableListenableFuture<T> implements ListenableFuture<T> {

	private final SettableFuture<T> settableFuture = new SettableFuture<T>();
	private final ListenableFutureCallbackRegistry<T> registry = new ListenableFutureCallbackRegistry<T>();


	/**
	 * Set the value of this future. This method will return {@code true} if
	 * the value was set successfully, or {@code false} if the future has already
	 * been set or cancelled.
	 *
	 * @param value the value that will be set.
	 * @return {@code true} if the value was successfully set, else {@code false}.
	 */
	public boolean set(T value) {
		boolean setValue = this.settableFuture.setValue(value);
		if (setValue) {
			this.registry.success(value);
		}
		return setValue;
	}

	/**
	 * Set the exception of this future. This method will return {@code true} if
	 * the exception was set successfully, or {@code false} if the future has already
	 * been set or cancelled.
	 * @param exception the value that will be set.
	 * @return {@code true} if the exception was successfully set, else {@code false}.
	 */
	public boolean setException(Throwable exception) {
		Assert.notNull(exception, "exception must not be null");
		boolean setException = this.settableFuture.setThrowable(exception);
		if (setException) {
			this.registry.failure(exception);
		}
		return setException;
	}

    @Override
    public void addCallback(ListenableFutureCallback<? super T> callback) {
		this.registry.addCallback(callback);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
		boolean cancelled = this.settableFuture.cancel(mayInterruptIfRunning);
		if (cancelled && mayInterruptIfRunning) {
			interruptTask();
		}
		return cancelled;
    }

    @Override
    public boolean isCancelled() {
        return this.settableFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.settableFuture.isDone();
    }

	/**
	 * Retrieve the value.
	 * <p>Will return the value if it has been set by calling {@link #set(Object)}, throw
	 * an {@link ExecutionException} if the {@link #setException(Throwable)} has been
	 * called, throw a {@link CancellationException} if the future has been cancelled, or
	 * throw an {@link IllegalStateException} if neither a value, nor an exception has
	 * been set.
	 * @return The value associated with this future.
	 */
    @Override
    public T get() throws InterruptedException, ExecutionException {
		return this.settableFuture.get();
    }

	/**
	 * Retrieve the value.
	 * <p>Will return the value if it has been by calling {@link #set(Object)}, throw an
	 * {@link ExecutionException} if the {@link #setException(Throwable)}
	 * has been called, throw a {@link java.util.concurrent.CancellationException} if the
	 * future has been cancelled.
	 * @param timeout the maximum time to wait.
	 * @param unit the time unit of the timeout argument.
	 * @return The value associated with this future.
	 */
	@Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.settableFuture.get(timeout, unit);
    }

	/**
	 * Subclasses can override this method to implement interruption of the future's
	 * computation. The method is invoked automatically by a successful call to
	 * {@link #cancel(boolean) cancel(true)}.
	 *
	 * <p>The default implementation does nothing.
	 */
	protected void interruptTask() {
	}


	/**
	 * Helper class that keeps track of the state of this future.
	 * @param <T> The type of value to be set.
	 */
	private static class SettableFuture<T> implements Future<T> {

		private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
		private final CountDownLatch latch = new CountDownLatch(1);
		private T value;
		private Throwable throwable;
		private State state = State.INITIALIZED;


		@Override
		public T get() throws ExecutionException, InterruptedException {
			this.latch.await();
			this.lock.readLock().lock();
			try {
				return getValue();
			}
			finally {
				this.lock.readLock().unlock();
			}
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			if (this.latch.await(timeout, unit)) {
				this.lock.readLock().lock();
				try {
					return getValue();
				}
				finally {
					this.lock.readLock().unlock();
				}
			}
			else {
				throw new TimeoutException();
			}
		}

		private T getValue() throws ExecutionException {
			switch (this.state) {
				case COMPLETED:
					if (this.throwable != null) {
						throw new ExecutionException(this.throwable);
					}
					else {
						return this.value;
					}
				case CANCELLED:
					throw new CancellationException("Future has been cancelled.");
				default:
					throw new IllegalStateException("Invalid state: " + this.state);
			}
		}

		@Override
		public boolean isDone() {
			this.lock.readLock().lock();
			try {
				switch (this.state) {
					case COMPLETED:
					case CANCELLED:
						return true;
					default:
						return false;
				}
			}
			finally {
				this.lock.readLock().unlock();
			}
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			this.lock.writeLock().lock();
			try {
				if (this.state.equals(State.INITIALIZED)) {
					this.state = State.CANCELLED;
					this.latch.countDown();
					return true;
				}
			}
			finally {
				this.lock.writeLock().unlock();
			}
			return false;
		}

		@Override
		public boolean isCancelled() {
			this.lock.readLock().lock();
			try {
				return this.state.equals(State.CANCELLED);
			}
			finally {
				this.lock.readLock().unlock();
			}
		}

		boolean setValue(T value) {
			this.lock.writeLock().lock();
			try {
				if (this.state.equals(State.INITIALIZED)) {
					this.value = value;
					this.state = State.COMPLETED;
					this.latch.countDown();
					return true;
				}
			}
			finally {
				this.lock.writeLock().unlock();
			}
			return false;
		}

		Throwable getThrowable() {
			return this.throwable;
		}

		boolean setThrowable(Throwable throwable) {
			this.lock.writeLock().lock();
			try {
				if (this.state.equals(State.INITIALIZED)) {
					this.throwable = throwable;
					this.state = State.COMPLETED;
					this.latch.countDown();
					return true;
				}
			}
			finally {
				this.lock.writeLock().unlock();
			}
			return false;
		}

		private enum State {INITIALIZED, COMPLETED, CANCELLED}
	}
}
