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

package org.springframework.util;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Composite iterator that combines multiple other iterators,
 * as registered via {@link #add(Iterator)}.
 *
 * <p>This implementation maintains a linked set of iterators
 * which are invoked in sequence until all iterators are exhausted.
 *
 * @author Erwin Vervaet
 * @author Juergen Hoeller
 * @since 3.0
 * @param <E> the element type
 */
public class CompositeIterator<E> implements Iterator<E> {

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
