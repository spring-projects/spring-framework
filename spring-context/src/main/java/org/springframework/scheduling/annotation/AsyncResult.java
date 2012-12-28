/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A pass-through {@code Future} handle that can be used for method signatures
 * which are declared with a Future return type for asynchronous execution.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see Async
 */
public class AsyncResult<V> implements Future<V> {

	private final V value;


	/**
	 * Create a new AsyncResult holder.
	 * @param value the value to pass through
	 */
	public AsyncResult(V value) {
		this.value = value;
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	public boolean isCancelled() {
		return false;
	}

	public boolean isDone() {
		return true;
	}

	public V get() {
		return this.value;
	}

	public V get(long timeout, TimeUnit unit) {
		return this.value;
	}

}
