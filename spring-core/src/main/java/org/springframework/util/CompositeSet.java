/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Composite set that combines two other sets. This type is only exposed through
 * {@link CompositeMap#keySet()} and {@link CompositeMap#entrySet()}.
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
