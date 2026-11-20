/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.comparator;

import java.util.Comparator;

/**
 * Comparator that adapts Comparables to the Comparator interface.
 * Mainly for internal use in other Comparators, when supposed
 * to work on Comparables.
 *
 * @author Keith Donald
 * @since 1.2.2
 * @param <T> the type of comparable objects that may be compared by this comparator
 * @see Comparable
 * @deprecated as of 6.1 in favor of {@link Comparator#naturalOrder()}
 */
@Deprecated(since = "6.1")
public class ComparableComparator<T extends Comparable<T>> implements Comparator<T> {

	/**
	 * A shared instance of this default comparator.
	 * @see Comparators#comparable()
	 */
	@SuppressWarnings("rawtypes")
	public static final ComparableComparator INSTANCE = new ComparableComparator();


	@Override
	public int compare(T o1, T o2) {
		return o1.compareTo(o2);
	}

}
