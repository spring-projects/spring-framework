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

/**
 * Comparator that adapts Comparables to the Comparator interface.
 * Mainly for internal use in other Comparators, when supposed
 * to work on Comparables.
 *
 * @author Keith Donald
 * @since 1.2.2
 * @see Comparable
 */
public class ComparableComparator<T extends Comparable<T>> implements Comparator<T> {

	@SuppressWarnings("rawtypes")
	public static final ComparableComparator INSTANCE = new ComparableComparator();

	protected ComparableComparator() {
		super();
	}

	/**
	 * Returns a type safe ComparableComparator instance.
	 *
	 * <p>This example illustrates the type-safe way to obtain an instance:
	 * <pre>
	 *     ComparableComparator&lt;Long&gt; s = ComparableComparator.get();
	 * </pre>
	 *
	 * @param <T> type of elements
	 * @return a ComparableComparator instance
	 *
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> ComparableComparator<T> get() {
		return (ComparableComparator<T>) INSTANCE;
	}

	@Override
	public int compare(T o1, T o2) {
		return o1.compareTo(o2);
	}

}
