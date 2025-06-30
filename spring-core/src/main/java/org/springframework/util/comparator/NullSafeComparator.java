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

package org.springframework.util.comparator;

import java.util.Comparator;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A Comparator that will safely compare nulls to be lower or higher than
 * other objects. Can decorate a given Comparator or work on Comparables.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @param <T> the type of objects that may be compared by this comparator
 * @see Comparable
 * @see Comparators
 * @deprecated as of 6.1 in favor of {@link Comparator#nullsLast} and {@link Comparator#nullsFirst}
 */
@Deprecated(since = "6.1")
public class NullSafeComparator<T> implements Comparator<T> {

	/**
	 * A shared default instance of this comparator, treating nulls lower
	 * than non-null objects.
	 * @see Comparators#nullsLow()
	 */
	@SuppressWarnings("rawtypes")
	public static final NullSafeComparator NULLS_LOW = new NullSafeComparator<>(true);

	/**
	 * A shared default instance of this comparator, treating nulls higher
	 * than non-null objects.
	 * @see Comparators#nullsHigh()
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
	 * <p>As a convenience, you can use the default shared instances:
	 * {@code NullSafeComparator.NULLS_LOW} and
	 * {@code NullSafeComparator.NULLS_HIGH}.
	 * @param nullsLow whether to treat nulls lower or higher than non-null objects
	 * @see Comparable
	 * @see #NULLS_LOW
	 * @see #NULLS_HIGH
	 */
	private NullSafeComparator(boolean nullsLow) {
		this.nonNullComparator = Comparators.comparable();
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
		Assert.notNull(comparator, "Comparator must not be null");
		this.nonNullComparator = comparator;
		this.nullsLow = nullsLow;
	}


	@Override
	public int compare(@Nullable T left, @Nullable T right) {
		Comparator<T> comparator = this.nullsLow ? Comparator.nullsFirst(this.nonNullComparator) : Comparator.nullsLast(this.nonNullComparator);
		return comparator.compare(left, right);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof NullSafeComparator<?> that &&
				this.nonNullComparator.equals(that.nonNullComparator) &&
				this.nullsLow == that.nullsLow));
	}

	@Override
	public int hashCode() {
		return Boolean.hashCode(this.nullsLow);
	}

	@Override
	public String toString() {
		return "NullSafeComparator: non-null comparator [" + this.nonNullComparator + "]; " +
				(this.nullsLow ? "nulls low" : "nulls high");
	}

}
