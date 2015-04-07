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

import java.util.concurrent.Future;

/**
 * Extends the {@link Future} interface with the capability to accept completion
 * callbacks. If the future has already completed when the callback is added, the
 * callback will be triggered immediately.
 * <p>Inspired by {@code com.google.common.util.concurrent.ListenableFuture}.

 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 4.0
 */
public interface ListenableFuture<T> extends Future<T> {

	/**
	 * Registers the given callback to this {@code ListenableFuture}. The callback will
	 * be triggered when this {@code Future} is complete or, if it is already complete,
	 * immediately.
	 * @param callback the callback to register
	 */
	void addCallback(ListenableFutureCallback<? super T> callback);

	/**
	 * Registers the given success and failure callbacks to this {@code ListenableFuture}.
	 * The callback will be triggered when this {@code Future} is complete or, if it is
	 * already complete immediately. This is a Java 8 lambdas compliant alternative to
	 * {@link #addCallback(ListenableFutureCallback)}.
	 * @param successCallback the success callback to register
	 * @param failureCallback the failure callback to register
	 * @since 4.1
	 */
	void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback);

}
