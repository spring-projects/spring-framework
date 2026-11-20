/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.comparator;

import java.io.Serializable;
import java.util.Comparator;

import org.jspecify.annotations.Nullable;

/**
 * A {@link Comparator} for {@link Boolean} objects that can sort either
 * {@code true} or {@code false} first.
 *
 * @author Keith Donald
 * @author Eugene Rabii
 * @since 1.2.2
 */
@SuppressWarnings("serial")
public class BooleanComparator implements Comparator<Boolean>, Serializable {

	/**
	 * A shared default instance of this comparator,
	 * treating {@code true} lower than {@code false}.
	 */
	public static final BooleanComparator TRUE_LOW = new BooleanComparator(true);

	/**
	 * A shared default instance of this comparator,
	 * treating {@code true} higher than {@code false}.
	 */
	public static final BooleanComparator TRUE_HIGH = new BooleanComparator(false);


	private final boolean trueLow;


	/**
	 * Create a BooleanComparator that sorts boolean values based on
	 * the provided flag.
	 * <p>Alternatively, you can use the default shared instances:
	 * {@code BooleanComparator.TRUE_LOW} and
	 * {@code BooleanComparator.TRUE_HIGH}.
	 * @param trueLow whether to treat true as lower or higher than false
	 * @see #TRUE_LOW
	 * @see #TRUE_HIGH
	 */
	public BooleanComparator(boolean trueLow) {
		this.trueLow = trueLow;
	}


	@Override
	public int compare(Boolean left, Boolean right) {
		int multiplier = this.trueLow ? -1 : 1;
		return multiplier * Boolean.compare(left, right);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof BooleanComparator that && this.trueLow == that.trueLow));
	}

	@Override
	public int hashCode() {
		return Boolean.hashCode(this.trueLow);
	}

	@Override
	public String toString() {
		return "BooleanComparator: " + (this.trueLow ? "true low" : "true high");
	}

}
