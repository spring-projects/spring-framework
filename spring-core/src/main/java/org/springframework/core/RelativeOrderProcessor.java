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

package org.springframework.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * A processor that sorts a list of components based on @Order, @DependsOnBefore, and @DependsOnAfter annotations.
 * This design separates the preparation phase (graph building and topological sort)
 * from the comparison phase, adhering to the standard Comparator contract.
 *
 * @author Yongjun Hong
 */
public final class RelativeOrderProcessor {

	/**
	 * hared default instance of {@code RelativeOrderProcessor}.
	 */
	public static final RelativeOrderProcessor INSTANCE = new RelativeOrderProcessor();

	private static final TopologicalOrderSolver TOPOLOGICAL_ORDER_SOLVER = new TopologicalOrderSolver();

	private RelativeOrderProcessor() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Sorts the given list of objects in place.
	 * @param list the list to be sorted.
	 */
	public static void sort(List<?> list) {
		if (list.size() <= 1) {
			return;
		}

		list.sort(getComparatorFor(list));
	}

	/**
	 * Creates a comparator tailored to the specific components in the input list.
	 * It pre-calculates the topological order for components with relative ordering needs.
	 * @param items the collection of items to create a comparator for.
	 * @return a fully configured Comparator.
	 */
	private static Comparator<Object> getComparatorFor(Collection<?> items) {
		List<Object> components = new ArrayList<>(items);

		Map<Object, Integer> orderMap = new HashMap<>();
		if (!components.isEmpty()) {
			List<Object> sortedRelative = TOPOLOGICAL_ORDER_SOLVER.resolveOrder(components);
			for (int i = 0; i < sortedRelative.size(); i++) {
				orderMap.put(sortedRelative.get(i), i);
			}
		}

		return new ConfiguredComparator(orderMap);
	}

	private static boolean hasRelativeOrderAnnotations(Object obj) {
		if (obj == null) {
			return false;
		}

		Class<?> clazz = (obj instanceof Class) ? (Class<?>) obj : obj.getClass();
		return AnnotationUtils.findAnnotation(clazz, DependsOnBefore.class) != null ||
				AnnotationUtils.findAnnotation(clazz, DependsOnAfter.class) != null;
	}

	/**
	 * The actual comparator implementation. It uses a pre-computed order map for relative
	 * components and falls back to AnnotationAwareOrderComparator for everything else.
	 */
	private static class ConfiguredComparator implements Comparator<Object> {
		private final Map<Object, Integer> orderMap;
		private final Comparator<Object> fallbackComparator = AnnotationAwareOrderComparator.INSTANCE;

		public ConfiguredComparator(Map<Object, Integer> orderMap) {
			this.orderMap = orderMap;
		}

		@Override
		public int compare(Object o1, Object o2) {
			boolean o1isRelative = hasRelativeOrderAnnotations(o1);
			boolean o2isRelative = hasRelativeOrderAnnotations(o2);

			// Case 1: Either components have relative order. Compare their topological index.
			if (o1isRelative || o2isRelative) {
				int order1 = this.orderMap.getOrDefault(o1, 0);
				int order2 = this.orderMap.getOrDefault(o2, 0);
				return Integer.compare(order1, order2);
			}

			// Case 2: One is relative, the other is absolute, or both are absolute.
			// Use the fallback comparator (@Order, @Priority) for tie-breaking.
			return this.fallbackComparator.compare(o1, o2);
		}
	}
}
