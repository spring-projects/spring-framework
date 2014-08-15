/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.List;

/**
 * Sort a collection of element according to an {@link OrderProvider}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface OrderProviderComparator {

	/**
	 * Sort the specified list of items according to their order value,
	 * using the specified {@link OrderProvider} to retrieve an order
	 * if necessary.
	 * @param items the items to sort
	 * @param orderProvider the order provider to use
	 * @see java.util.Collections#sort(java.util.List, java.util.Comparator)
	 */
	void sortList(List<?> items, OrderProvider orderProvider);

	/**
	 * Sort the specified array of items according to their order value,
	 * using the specified {@link OrderProvider} to retrieve an order
	 * if necessary.
	 * @param items the items to sort
	 * @param orderProvider the order provider to use
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	void sortArray(Object[] items, OrderProvider orderProvider);

}
