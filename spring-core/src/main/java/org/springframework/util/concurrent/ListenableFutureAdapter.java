/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * Abstract class that adapts a {@link ListenableFuture} parameterized over S into a
 * {@code ListenableFuture} parameterized over T. All methods are delegated to the
 * adaptee, where {@link #get()}, {@link #get(long, java.util.concurrent.TimeUnit)},
 * and {@link ListenableFutureCallback#onSuccess(Object)} call {@link #adapt(Object)}
 * on the adaptee's result.
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @param <T> the type of this {@code Future}
 * @param <S> the type of the adaptee's {@code Future}
 * @deprecated as of 6.0, in favor of
 * {@link java.util.concurrent.CompletableFuture}
 */
@Deprecated(since = "6.0")
public abstract class ListenableFutureAdapter<T, S> extends FutureAdapter<T, S> implements ListenableFuture<T> {

	/**
	 * Construct a new {@code ListenableFutureAdapter} with the given adaptee.
	 * @param adaptee the future to adapt to
	 */
	protected ListenableFutureAdapter(ListenableFuture<S> adaptee) {
		super(adaptee);
	}


	@Override
	public void addCallback(final ListenableFutureCallback<? super T> callback) {
		addCallback(callback, callback);
	}

	@Override
	public void addCallback(final SuccessCallback<? super T> successCallback, final FailureCallback failureCallback) {
		ListenableFuture<S> listenableAdaptee = (ListenableFuture<S>) getAdaptee();
		listenableAdaptee.addCallback(new ListenableFutureCallback<>() {
			@Override
			public void onSuccess(@Nullable S result) {
				T adapted = null;
				if (result != null) {
					try {
						adapted = adaptInternal(result);
					}
					catch (ExecutionException ex) {
						Throwable cause = ex.getCause();
						onFailure(cause != null ? cause : ex);
						return;
					}
					catch (Throwable ex) {
						onFailure(ex);
						return;
					}
				}
				successCallback.onSuccess(adapted);
			}

			@Override
			public void onFailure(Throwable ex) {
				failureCallback.onFailure(ex);
			}
		});
	}

}
