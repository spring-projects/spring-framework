/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.concurrent.Future;

import org.springframework.util.Assert;

/**
 * Extension of {@link CompletableFuture} which allows for cancelling
 * a delegate along with the {@link CompletableFuture} itself.
 *
 * @author Juergen Hoeller
 * @since 5.0
 * @param <T> the result type returned by this Future's {@code get} method
 */
class DelegatingCompletableFuture<T> extends CompletableFuture<T> {

	private final Future<T> delegate;


	public DelegatingCompletableFuture(Future<T> delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}


	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		boolean result = this.delegate.cancel(mayInterruptIfRunning);
		super.cancel(mayInterruptIfRunning);
		return result;
	}

}
