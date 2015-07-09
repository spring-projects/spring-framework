/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.reactive.util;

import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
class OnNext<T> implements Signal<T> {

	private final T next;

	public OnNext(T next) {
		Assert.notNull(next, "'next' must not be null");
		this.next = next;
	}

	@Override
	public boolean isOnNext() {
		return true;
	}

	@Override
	public T next() {
		return next;
	}

	@Override
	public boolean isOnError() {
		return false;
	}

	@Override
	public Throwable error() {
		throw new IllegalStateException();
	}

	@Override
	public boolean isComplete() {
		return false;
	}
}
