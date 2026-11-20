/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.comparator;

import java.util.Comparator;

/**
 * Convenient entry point with generically typed factory methods
 * for common Spring {@link Comparator} variants.
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
public abstract class Comparators {

	/**
	 * Return a {@link Comparable} adapter.
	 * @see Comparator#naturalOrder()
	 */
	@SuppressWarnings("unchecked")
	public static <T> Comparator<T> comparable() {
		return (Comparator<T>) Comparator.naturalOrder();
	}

	/**
	 * Return a {@link Comparable} adapter which accepts
	 * null values and sorts them lower than non-null values.
	 * @see Comparator#nullsFirst(Comparator)
	 */
	public static <T> Comparator<T> nullsLow() {
		return nullsLow(comparable());
	}

	/**
	 * Return a decorator for the given comparator which accepts
	 * null values and sorts them lower than non-null values.
	 * @see Comparator#nullsFirst(Comparator)
	 */
	public static <T> Comparator<T> nullsLow(Comparator<T> comparator) {
		return Comparator.nullsFirst(comparator);
	}

	/**
	 * Return a {@link Comparable} adapter which accepts
	 * null values and sorts them higher than non-null values.
	 * @see Comparator#nullsLast(Comparator)
	 */
	public static <T> Comparator<T> nullsHigh() {
		return nullsHigh(comparable());
	}

	/**
	 * Return a decorator for the given comparator which accepts
	 * null values and sorts them higher than non-null values.
	 * @see Comparator#nullsLast(Comparator)
	 */
	public static <T> Comparator<T> nullsHigh(Comparator<T> comparator) {
		return Comparator.nullsLast(comparator);
	}

}
