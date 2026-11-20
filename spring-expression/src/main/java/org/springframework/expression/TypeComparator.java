/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

import org.jspecify.annotations.Nullable;

/**
 * Instances of a type comparator should be able to compare pairs of objects for equality.
 * The specification of the return value is the same as for {@link java.lang.Comparable}.
 *
 * @author Andy Clement
 * @since 3.0
 * @see java.lang.Comparable
 */
public interface TypeComparator {

	/**
	 * Return {@code true} if the comparator can compare these two objects.
	 * @param firstObject the first object
	 * @param secondObject the second object
	 * @return {@code true} if the comparator can compare these objects
	 */
	boolean canCompare(@Nullable Object firstObject, @Nullable Object secondObject);

	/**
	 * Compare two given objects.
	 * @param firstObject the first object
	 * @param secondObject the second object
	 * @return 0 if they are equal, a negative integer if the first is smaller than
	 * the second, or a positive integer if the first is larger than the second
	 * @throws EvaluationException if a problem occurs during comparison
	 * (or if they are not comparable in the first place)
	 * @see Comparable#compareTo
	 */
	int compare(@Nullable Object firstObject, @Nullable Object secondObject) throws EvaluationException;

}
