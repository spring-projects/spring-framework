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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A default {@link OrderProviderComparator} implementation that uses the
 * value provided by the {@link OrderProvider} and fallbacks to
 * {@link AnnotationAwareOrderComparator} if none is set.
 *
 * <p>This essentially means that the value of the {@link OrderProvider}
 * takes precedence over the behavior of {@link AnnotationAwareOrderComparator}
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class DefaultOrderProviderComparator implements OrderProviderComparator {

	/**
	 * Shared default instance of DefaultOrderProviderComparator.
	 */
	public static final DefaultOrderProviderComparator INSTANCE = new DefaultOrderProviderComparator();

	@Override
	public void sortList(List<?> items, OrderProvider orderProvider) {
		Collections.sort(items, new OrderProviderAwareComparator(orderProvider));
	}

	@Override
	public void sortArray(Object[] items, OrderProvider orderProvider) {
		Arrays.sort(items, new OrderProviderAwareComparator(orderProvider));
	}


	private static class OrderProviderAwareComparator extends AnnotationAwareOrderComparator {

		private final OrderProvider orderProvider;

		private OrderProviderAwareComparator(OrderProvider orderProvider) {
			this.orderProvider = orderProvider;
		}

		@Override
		protected int getOrder(Object obj) {
			Integer order = this.orderProvider.getOrder(obj);
			if (order != null) {
				return order;
			}
			return super.getOrder(obj);
		}
	}

}
