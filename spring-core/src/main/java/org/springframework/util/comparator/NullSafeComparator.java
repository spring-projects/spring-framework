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

package org.springframework.util.comparator;

import java.util.Comparator;

import org.springframework.util.Assert;

/**
 * A Comparator that will safely compare nulls to be lower or higher than
 * other objects. Can decorate a given Comparator or work on Comparables.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @see Comparable
 */
public class NullSafeComparator<T extends Comparable<T>> implements Comparator<T> {

	/**
	 * A shared default instance of this comparator, treating nulls lower
	 * than non-null objects.
	 */
	@SuppressWarnings("rawtypes")
	public static final NullSafeComparator NULLS_LOW = new NullSafeComparator<>(true);

	/**
	 * A shared default instance of this comparator, treating nulls higher
	 * than non-null objects.
	 */
	@SuppressWarnings("rawtypes")
	public static final NullSafeComparator NULLS_HIGH = new NullSafeComparator<>(false);

	private final Comparator<T> nonNullComparator;

	private final boolean nullsLow;


	/**
	 * Create a NullSafeComparator that sorts {@code null} based on
	 * the provided flag, working on Comparables.
	 * <p>When comparing two non-null objects, their Comparable implementation
	 * will be used: this means that non-null elements (that this Comparator
	 * will be applied to) need to implement Comparable.
	 * <p>As a convenience, you can use the type safe methods:
	 * {@code NullSafeComparator.nullsLow()} and
	 * {@code NullSafeComparator.nullsHigh()}.
	 * @param nullsLow whether to treat nulls lower or higher than non-null objects
	 * @see Comparable
	 * @see #nullsLow()
	 * @see #nullsHigh()
	 */
	private NullSafeComparator(boolean nullsLow) {
		this.nonNullComparator = ComparableComparator.get();
		this.nullsLow = nullsLow;
	}

	/**
	 * Create a NullSafeComparator that sorts {@code null} based on the
	 * provided flag, decorating the given Comparator.
	 * <p>When comparing two non-null objects, the specified Comparator will be used.
	 * The given underlying Comparator must be able to handle the elements that this
	 * Comparator will be applied to.
	 * @param comparator the comparator to use when comparing two non-null objects
	 * @param nullsLow whether to treat nulls lower or higher than non-null objects
	 */
	public NullSafeComparator(Comparator<T> comparator, boolean nullsLow) {
		Assert.notNull(comparator, "The non-null comparator is required");
		this.nonNullComparator = comparator;
		this.nullsLow = nullsLow;
	}

	/**
	 * Returns a type safe instance of this comparator, treating nulls lower
	 * than non-null objects.
	 *
	 * <p>This example illustrates the type-safe way to obtain an instance:
	 * <pre>
	 *     NullSafeComparator&lt;Date&gt; s = NullSafeComparator.nullsLow();
	 * </pre>
	 *
	 * @param <T> type of elements
	 * @return a NullSafeComparator instance
	 *
	 */
	@SuppressWarnings("unchecked")
	public static final <T extends Comparable<T>> NullSafeComparator<T> nullsLow() {
		return (NullSafeComparator<T>) NULLS_LOW;
	}

	/**
	 * Returns a type safe instance of this comparator, treating nulls higher
	 * than non-null objects.
	 *
	 * <p>This example illustrates the type-safe way to obtain an instance:
	 * <pre>
	 *     NullSafeComparator&lt;Date&gt; s = NullSafeComparator.nullsHigh();
	 * </pre>
	 *
	 * @param <T> type of elements
	 * @return a NullSafeComparator instance
	 *
	 */
	@SuppressWarnings("unchecked")
	public static final <T extends Comparable<T>> NullSafeComparator<T> nullsHigh() {
		return (NullSafeComparator<T>) NULLS_HIGH;
	}

	@Override
	public int compare(T o1, T o2) {
		if (o1 == o2) {
			return 0;
		}
		if (o1 == null) {
			return (this.nullsLow ? -1 : 1);
		}
		if (o2 == null) {
			return (this.nullsLow ? 1 : -1);
		}
		return this.nonNullComparator.compare(o1, o2);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NullSafeComparator<?>)) {
			return false;
		}
		NullSafeComparator<?> other = (NullSafeComparator<?>) obj;
		return (this.nonNullComparator.equals(other.nonNullComparator) && this.nullsLow == other.nullsLow);
	}

	@Override
	public int hashCode() {
		return (this.nullsLow ? -1 : 1) * this.nonNullComparator.hashCode();
	}

	@Override
	public String toString() {
		return "NullSafeComparator: non-null comparator [" + this.nonNullComparator + "]; " +
				(this.nullsLow ? "nulls low" : "nulls high");
	}

}
