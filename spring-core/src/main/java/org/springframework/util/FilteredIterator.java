/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * @author Yanming Zhou
 * @since 6.2
 * @param <E> the type of elements returned by this iterator
 */
final class FilteredIterator<E extends @Nullable Object> implements Iterator<E> {

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
	public @Nullable E next() {
		if (!this.hasNext && !setNext()) {
			throw new NoSuchElementException();
		}
		this.hasNext = false;
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
