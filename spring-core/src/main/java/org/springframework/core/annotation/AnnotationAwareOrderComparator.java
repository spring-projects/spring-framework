/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.core.annotation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;

/**
 * {@link java.util.Comparator} implementation that checks
 * {@link org.springframework.core.Ordered} as well as the
 * {@link Order} annotation, with an order value provided by an
 * {@code Ordered} instance overriding a statically defined
 * annotation value (if any).
 *
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @since 2.0.1
 * @see org.springframework.core.Ordered
 * @see Order
 */
public class AnnotationAwareOrderComparator extends OrderComparator {

	/**
	 * Shared default instance of AnnotationAwareOrderComparator.
	 */
	public static final AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();


	@Override
	protected int getOrder(Object obj) {
		if (obj instanceof Ordered) {
			return ((Ordered) obj).getOrder();
		}
		if (obj != null) {
			Order order = obj.getClass().getAnnotation(Order.class);
			if (order != null) {
				return order.value();
			}
		}
		return Ordered.LOWEST_PRECEDENCE;
	}


	/**
	 * Sort the given List with a default AnnotationAwareOrderComparator.
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * @param list the List to sort
	 * @see java.util.Collections#sort(java.util.List, java.util.Comparator)
	 */
	public static void sort(List<?> list) {
		if (list.size() > 1) {
			Collections.sort(list, INSTANCE);
		}
	}

	/**
	 * Sort the given array with a default AnnotationAwareOrderComparator.
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * @param array the array to sort
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	public static void sort(Object[] array) {
		if (array.length > 1) {
			Arrays.sort(array, INSTANCE);
		}
	}

}
