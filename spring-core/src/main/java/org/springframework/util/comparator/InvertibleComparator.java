/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.Serializable;
import java.util.Comparator;

import org.springframework.util.Assert;

/**
 * A decorator for a comparator, with an "ascending" flag denoting
 * whether comparison results should be treated in forward (standard
 * ascending) order or flipped for reverse (descending) order.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @param <T> the type of objects that may be compared by this comparator
 * @deprecated as of Spring Framework 5.0, in favor of the standard JDK 8
 * {@link Comparator#reversed()}
 */
@Deprecated
@SuppressWarnings("serial")
public class InvertibleComparator<T> implements Comparator<T>, Serializable {

	private final Comparator<T> comparator;

	private boolean ascending = true;


	/**
	 * Create an InvertibleComparator that sorts ascending by default.
	 * For the actual comparison, the specified Comparator will be used.
	 * @param comparator the comparator to decorate
	 */
	public InvertibleComparator(Comparator<T> comparator) {
		Assert.notNull(comparator, "Comparator must not be null");
		this.comparator = comparator;
	}

	/**
	 * Create an InvertibleComparator that sorts based on the provided order.
	 * For the actual comparison, the specified Comparator will be used.
	 * @param comparator the comparator to decorate
	 * @param ascending the sort order: ascending (true) or descending (false)
	 */
	public InvertibleComparator(Comparator<T> comparator, boolean ascending) {
		Assert.notNull(comparator, "Comparator must not be null");
		this.comparator = comparator;
		setAscending(ascending);
	}


	/**
	 * Specify the sort order: ascending (true) or descending (false).
	 */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	/**
	 * Return the sort order: ascending (true) or descending (false).
	 */
	public boolean isAscending() {
		return this.ascending;
	}

	/**
	 * Invert the sort order: ascending -> descending or
	 * descending -> ascending.
	 */
	public void invertOrder() {
		this.ascending = !this.ascending;
	}


	@Override
	public int compare(T o1, T o2) {
		int result = this.comparator.compare(o1, o2);
		if (result != 0) {
			// Invert the order if it is a reverse sort.
			if (!this.ascending) {
				if (Integer.MIN_VALUE == result) {
					result = Integer.MAX_VALUE;
				}
				else {
					result *= -1;
				}
			}
			return result;
		}
		return 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof InvertibleComparator)) {
			return false;
		}
		InvertibleComparator<T> otherComp = (InvertibleComparator<T>) other;
		return (this.comparator.equals(otherComp.comparator) && this.ascending == otherComp.ascending);
	}

	@Override
	public int hashCode() {
		return this.comparator.hashCode();
	}

	@Override
	public String toString() {
		return "InvertibleComparator: [" + this.comparator + "]; ascending=" + this.ascending;
	}

}
