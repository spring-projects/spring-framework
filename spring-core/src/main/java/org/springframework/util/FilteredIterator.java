/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

/**
 * {@link Iterator} that filters out values that do not match a predicate.
 *
 * <p>This type is used by {@link CompositeMap}.
 *
 * @author Arjen Poutsma
 * @since 6.2
 * @param <E> the type of elements returned by this iterator
 */
final class FilteredIterator<E> implements Iterator<E> {

	private final Iterator<E> delegate;

	private final Predicate<E> filter;

	private @Nullable E next;

	private boolean hasNext;


	public FilteredIterator(Iterator<E> delegate, Predicate<E> filter) {
		Assert.notNull(delegate, "Delegate must not be null");
		Assert.notNull(filter, "Filter must not be null");
		this.delegate = delegate;
		this.filter = filter;
	}


	@Override
	public boolean hasNext() {
		return (this.hasNext || setNext());
	}

	@Override
	public E next() {
		if (!this.hasNext && !setNext()) {
			throw new NoSuchElementException();
		}
		this.hasNext = false;
		Assert.state(this.next != null, "Next should not be null");
		return this.next;
	}

	private boolean setNext() {
		while (this.delegate.hasNext()) {
			E next = this.delegate.next();
			if (this.filter.test(next)) {
				this.next = next;
				this.hasNext = true;
				return true;
			}
		}
		return false;
	}

}
