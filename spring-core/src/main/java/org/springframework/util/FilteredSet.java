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
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

/**
 * Set that filters out values that do not match a predicate.
 * This type is used by {@link CompositeMap}.
 *
 * @author Arjen Poutsma
 * @since 6.2
 * @param <E> the type of elements maintained by this set
 */
final class FilteredSet<E> extends FilteredCollection<E> implements Set<E> {

	public FilteredSet(Set<E> delegate, Predicate<E> filter) {
		super(delegate, filter);
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
