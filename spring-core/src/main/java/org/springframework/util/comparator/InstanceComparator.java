/*
 * Copyright 2002-2015 the original author or authors.
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
 * Compares objects based on an arbitrary class order. Allows objects to be sorted based
 * on the types of class that they inherit, for example: this comparator can be used to
 * sort a list {@code Number}s such that {@code Long}s occur before {@code Integer}s.
 *
 * <p>Only the specified {@code instanceOrder} classes are considered during comparison.
 * If two objects are both instances of the ordered type this comparator will return a
 * {@code 0}. Consider combining with a {@link CompoundComparator} if additional sorting
 * is required.
 *
 * @author Phillip Webb
 * @since 3.2
 * @see CompoundComparator
 * @param <T> the type of objects being compared
 */
public class InstanceComparator<T> implements Comparator<T> {

	private final Class<?>[] instanceOrder;


	/**
	 * Create a new {@link InstanceComparator} instance.
	 * @param instanceOrder the ordered list of classes that should be used when comparing
	 * objects. Classes earlier in the list will be be given a higher priority.
	 */
	public InstanceComparator(Class<?>... instanceOrder) {
		Assert.notNull(instanceOrder, "'instanceOrder' must not be null");
		this.instanceOrder = instanceOrder;
	}


	@Override
	public int compare(T o1, T o2) {
		int i1 = getOrder(o1);
		int i2 = getOrder(o2);
		return (i1 < i2 ? -1 : (i1 == i2 ? 0 : 1));
	}

	private int getOrder(T object) {
		if (object != null) {
			for (int i = 0; i < this.instanceOrder.length; i++) {
				if (this.instanceOrder[i].isInstance(object)) {
					return i;
				}
			}
		}
		return this.instanceOrder.length;
	}

}
