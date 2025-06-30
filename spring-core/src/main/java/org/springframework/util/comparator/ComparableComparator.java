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
