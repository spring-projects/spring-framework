/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Composite iterator that combines multiple other iterators,
 * as registered via {@link #add(Iterator)}.
 *
 * <p>This implementation maintains a linked set of iterators
 * which are invoked in sequence until all iterators are exhausted.
 *
 * @author Erwin Vervaet
 * @author Juergen Hoeller
 * @author Yanming Zhou
 * @since 3.0
 * @param <E> the element type
 */
public class CompositeIterator<E extends @Nullable Object> implements Iterator<E> {

	private final Set<Iterator<E>> iterators = new LinkedHashSet<>();

	private boolean inUse = false;


	/**
	 * Add given iterator to this composite.
	 */
	public void add(Iterator<E> iterator) {
		Assert.state(!this.inUse, "You can no longer add iterators to a composite iterator that's already in use");
		if (this.iterators.contains(iterator)) {
			throw new IllegalArgumentException("You cannot add the same iterator twice");
		}
		this.iterators.add(iterator);
	}

	@Override
	public boolean hasNext() {
		this.inUse = true;
		for (Iterator<E> iterator : this.iterators) {
			if (iterator.hasNext()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public E next() {
		this.inUse = true;
		for (Iterator<E> iterator : this.iterators) {
			if (iterator.hasNext()) {
				return iterator.next();
			}
		}
		throw new NoSuchElementException("All iterators exhausted");
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("CompositeIterator does not support remove()");
	}

}
