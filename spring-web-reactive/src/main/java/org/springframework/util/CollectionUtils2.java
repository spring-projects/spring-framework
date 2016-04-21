/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Enumeration;
import java.util.Iterator;

/**
 * TODO: to be merged with {@link CollectionUtils}
 * @author Arjen Poutsma
 */
public abstract class CollectionUtils2 {

	/**
	 * Adapt an iterator to an enumeration.
	 * @param iterator the iterator
	 * @return the enumeration
	 */
	public static <E> Enumeration<E> toEnumeration(Iterator<E> iterator) {
		return new IteratorEnumeration<E>(iterator);
	}

	/**
	 * Enumeration wrapping an Iterator.
	 */
	private static class IteratorEnumeration<T> implements Enumeration<T> {

		private final Iterator<T> iterator;

		public IteratorEnumeration(Iterator<T> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasMoreElements() {
			return this.iterator.hasNext();
		}

		@Override
		public T nextElement() {
			return this.iterator.next();
		}
	}


}
