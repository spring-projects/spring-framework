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
import java.util.List;

import org.springframework.core.annotation.AnnotationUtils;

/**
 * Resolves relative ordering relationships using a topological sort.
 *
 * @author Yongjun Hong
 */
public class TopologicalOrderSolver {

	/**
	 * Resolves the relative order of the given components and returns a sorted list.
	 */
	public List<Object> resolveOrder(List<Object> components) {
		if (components.size() <= 1) {
			return new ArrayList<>(components);
		}
		OrderGraph graph = buildOrderGraph(components);
		return graph.topologicalSort();
	}

	/**
	 * Builds the ordering relationship graph from the given components.
	 */
	private OrderGraph buildOrderGraph(List<Object> components) {
		OrderGraph graph = new OrderGraph();

		for (Object component : components) {
			graph.addNode(component);
		}

		for (Object component : components) {
			addOrderConstraints(graph, component);
		}
		return graph;
	}

	private void addOrderConstraints(OrderGraph graph, Object component) {
		if (component == null) {
			return;
		}
		Class<?> componentClass = (component instanceof Class) ? (Class<?>) component : component.getClass();

		// Process @DependsOnBefore
		DependsOnBefore dependsOnBefore = AnnotationUtils.findAnnotation(componentClass, DependsOnBefore.class);
		if (dependsOnBefore != null) {
			for (Class<?> beforeClass : dependsOnBefore.value()) {
				graph.addEdge(component, beforeClass);
			}
		}

		// Process @DependsOnAfter
		DependsOnAfter dependsOnAfter = AnnotationUtils.findAnnotation(componentClass, DependsOnAfter.class);
		if (dependsOnAfter != null) {
			for (Class<?> afterClass : dependsOnAfter.value()) {
				graph.addEdge(afterClass, component);
			}
		}
	}
}
