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

package org.springframework.util.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Abstract class that adapts a {@link Future} parameterized over S into a {@code Future}
 * parameterized over T. All methods are delegated to the adaptee, where {@link #get()}
 * and {@link #get(long, TimeUnit)} call {@link #adapt(Object)} on the adaptee's result.
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @param <T> the type of this {@code Future}
 * @param <S> the type of the adaptee's {@code Future}
 * @deprecated as of 6.0, with no concrete replacement
 */
@Deprecated(since = "6.0")
public abstract class FutureAdapter<T, S> implements Future<T> {

	private final Future<S> adaptee;

	private @Nullable Object result;

	private State state = State.NEW;

	private final Object mutex = new Object();


	/**
	 * Constructs a new {@code FutureAdapter} with the given adaptee.
	 * @param adaptee the future to delegate to
	 */
	protected FutureAdapter(Future<S> adaptee) {
		Assert.notNull(adaptee, "Delegate must not be null");
		this.adaptee = adaptee;
	}


	/**
	 * Returns the adaptee.
	 */
	protected Future<S> getAdaptee() {
		return this.adaptee;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return this.adaptee.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return this.adaptee.isCancelled();
	}

	@Override
	public boolean isDone() {
		return this.adaptee.isDone();
	}

	@Override
	public @Nullable T get() throws InterruptedException, ExecutionException {
		return adaptInternal(this.adaptee.get());
	}

	@Override
	public @Nullable T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return adaptInternal(this.adaptee.get(timeout, unit));
	}

	@SuppressWarnings("unchecked")
	final @Nullable T adaptInternal(S adapteeResult) throws ExecutionException {
		synchronized (this.mutex) {
			return switch (this.state) {
				case SUCCESS -> (T) this.result;
				case FAILURE -> {
					Assert.state(this.result instanceof ExecutionException, "Failure without exception");
					throw (ExecutionException) this.result;
				}
				case NEW -> {
					try {
						T adapted = adapt(adapteeResult);
						this.result = adapted;
						this.state = State.SUCCESS;
						yield adapted;
					}
					catch (ExecutionException ex) {
						this.result = ex;
						this.state = State.FAILURE;
						throw ex;
					}
					catch (Throwable ex) {
						ExecutionException execEx = new ExecutionException(ex);
						this.result = execEx;
						this.state = State.FAILURE;
						throw execEx;
					}
				}
			};
		}
	}

	/**
	 * Adapts the given adaptee's result into T.
	 * @return the adapted result
	 */
	protected abstract @Nullable T adapt(S adapteeResult) throws ExecutionException;


	private enum State {NEW, SUCCESS, FAILURE}

}
