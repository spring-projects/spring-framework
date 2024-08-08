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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;


/**
 * Composite collection that combines two other collections. This type is only
 * exposed through {@link CompositeMap#values()}.
 *
 * @author Arjen Poutsma
 * @since 6.2
 * @param <E> the type of elements maintained by this collection
 */
class CompositeCollection<E> implements Collection<E> {

	private final Collection<E> first;

	private final Collection<E> second;


	CompositeCollection(Collection<E> first, Collection<E> second) {
		Assert.notNull(first, "First must not be null");
		Assert.notNull(second, "Second must not be null");
		this.first = first;
		this.second = second;
	}

	@Override
	public int size() {
		return this.first.size() + this.second.size();
	}

	@Override
	public boolean isEmpty() {
		return this.first.isEmpty() && this.second.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		if (this.first.contains(o)) {
			return true;
		}
		else {
			return this.second.contains(o);
		}
	}

	@Override
	public Iterator<E> iterator() {
		CompositeIterator<E> iterator = new CompositeIterator<>();
		iterator.add(this.first.iterator());
		iterator.add(this.second.iterator());
		return iterator;
	}

	@Override
	public Object[] toArray() {
		Object[] result = new Object[size()];
		Object[] firstArray = this.first.toArray();
		Object[] secondArray = this.second.toArray();
		System.arraycopy(firstArray, 0, result, 0, firstArray.length);
		System.arraycopy(secondArray, 0, result, firstArray.length, secondArray.length);
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		int size = this.size();
		T[] result;
		if (a.length >= size) {
			result = a;
		}
		else {
			result = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
		}

		int idx = 0;
		for (E e : this) {
			result[idx++] = (T) e;
		}
		if (result.length > size) {
			result[size] = null;
		}
		return result;
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		boolean firstResult = this.first.remove(o);
		boolean secondResult = this.second.remove(o);
		return firstResult || secondResult;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean changed = false;
		for (E e : c) {
			if (add(e)) {
				changed = true;
			}
		}
		return changed;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (c.isEmpty()) {
			return false;
		}
		boolean firstResult = this.first.removeAll(c);
		boolean secondResult = this.second.removeAll(c);

		return firstResult || secondResult;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean firstResult = this.first.retainAll(c);
		boolean secondResult = this.second.retainAll(c);

		return firstResult || secondResult;
	}

	@Override
	public void clear() {
		this.first.clear();
		this.second.clear();
	}
}
