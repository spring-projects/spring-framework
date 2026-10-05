/*
 * Copyright 2002-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

/**
 * Composite set that combines two other sets. This type is only exposed through
 * {@link CompositeMap#keySet()} and {@link CompositeMap#entrySet()}.
 *
 * <p><strong>WARNING</strong>: The two sets must be disjoint. Elements are not
 * deduplicated, so an element present in both sets would be counted and iterated
 * twice, which violates the {@link Set} contract. {@link CompositeMap} satisfies
 * this requirement by wrapping its second map in a {@link FilteredMap} that hides
 * keys present in the first map.
 *
 * @author Arjen Poutsma
 * @author Yanming Zhou
 * @since 6.2
 * @param <E> the type of elements maintained by this set
 */
final class CompositeSet<E extends @Nullable Object> extends CompositeCollection<E> implements Set<E> {

	CompositeSet(Set<E> first, Set<E> second) {
		super(first, second);
	}


	@Override
	public boolean remove(Object o) {
		// A set contains a given element at most once: remove it from both sets
		// so that an element hidden in the second set does not become visible.
		boolean firstResult = this.first.remove(o);
		boolean secondResult = this.second.remove(o);
		return (firstResult || secondResult);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other instanceof Set<?> otherSet && size() == otherSet.size()) {
			try {
				return containsAll(otherSet);
			}
			catch (ClassCastException | NullPointerException ignored) {
				// fall through
			}
		}
		return false;
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
