/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * @author Yanming Zhou
 * @since 6.2
 * @param <E> the type of elements maintained by this set
 */
final class FilteredSet<E extends @Nullable Object> extends FilteredCollection<E> implements Set<E> {

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
