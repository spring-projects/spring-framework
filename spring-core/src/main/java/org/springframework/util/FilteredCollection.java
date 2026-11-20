/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

/**
 * Collection that filters out values that do not match a predicate.
 * This type is used by {@link CompositeMap}.
 *
 * @author Arjen Poutsma
 * @author Yanming Zhou
 * @since 6.2
 * @param <E> the type of elements maintained by this collection
 */
class FilteredCollection<E extends @Nullable Object> extends AbstractCollection<E> {

	private final Collection<E> delegate;

	private final Predicate<E> filter;


	public FilteredCollection(Collection<E> delegate, Predicate<E> filter) {
		Assert.notNull(delegate, "Delegate must not be null");
		Assert.notNull(filter, "Filter must not be null");
		this.delegate = delegate;
		this.filter = filter;
	}


	@Override
	public int size() {
		int size = 0;
		for (E e : this.delegate) {
			if (this.filter.test(e)) {
				size++;
			}
		}
		return size;
	}

	@Override
	public Iterator<E> iterator() {
		return new FilteredIterator<>(this.delegate.iterator(), this.filter);
	}

	@Override
	public boolean add(E e) {
		boolean added = this.delegate.add(e);
		return added && this.filter.test(e);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object o) {
		boolean removed = this.delegate.remove(o);
		return removed && this.filter.test((E) o);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean contains(Object o) {
		if (this.delegate.contains(o)) {
			return this.filter.test((E) o);
		}
		else {
			return false;
		}
	}

	@Override
	public void clear() {
		this.delegate.clear();
	}

}
