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

import java.util.Set;

/**
 * Composite set that combines two other sets. This type is only exposed through
 * {@link CompositeMap#keySet()} and {@link CompositeMap#entrySet()}.
 *
 * @author Arjen Poutsma
 * @since 6.2
 * @param <E> the type of elements maintained by this set
 */
final class CompositeSet<E> extends CompositeCollection<E> implements Set<E> {

	CompositeSet(Set<E> first, Set<E> second) {
		super(first, second);
	}


	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		else if (obj instanceof Set<?> set) {
			if (set.size() != size()) {
				return false;
			}
			try {
				return containsAll(set);
			}
			catch (ClassCastException | NullPointerException ignored) {
				return false;
			}
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		for (E obj : this) {
			if (obj != null) {
				hashCode += obj.hashCode();
			}
		}
		return hashCode;
	}
}
