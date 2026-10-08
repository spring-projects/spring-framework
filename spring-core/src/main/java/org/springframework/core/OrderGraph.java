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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * A directed graph to represent relative ordering relationships.
 *
 * @author Yongjun Hong
 */
public class OrderGraph {

	private final Map<Object, Set<Object>> adjacencyList = new HashMap<>();
	private final Map<Object, Integer> inDegree = new HashMap<>();
	private final Set<Object> allNodes = new HashSet<>();

	public void addNode(Object node) {
		this.allNodes.add(node);
		this.adjacencyList.putIfAbsent(node, new HashSet<>());
		this.inDegree.putIfAbsent(node, 0);
	}

	/**
	 * Adds an edge indicating that 'from' must be ordered before 'to'.
	 */
	public void addEdge(Object from, Object to) {
		addNode(from);
		addNode(to);

		Set<Object> neighbors = this.adjacencyList.get(from);
		if (neighbors != null && neighbors.add(to)) {
			this.inDegree.put(to, this.inDegree.getOrDefault(to, 0) + 1);
		}
	}

	/**
	 * Performs a topological sort using Kahn's algorithm.
	 */
	public List<Object> topologicalSort() {
		Map<Object, Integer> tempInDegree = new HashMap<>(this.inDegree);
		Queue<Object> queue = new LinkedList<>();
		List<Object> result = new ArrayList<>();

		for (Object node : this.allNodes) {
			if (tempInDegree.getOrDefault(node, 0) == 0) {
				queue.offer(node);
			}
		}

		while (!queue.isEmpty()) {
			Object current = queue.poll();
			result.add(current);

			for (Object neighbor : this.adjacencyList.getOrDefault(current, Collections.emptySet())) {
				tempInDegree.put(neighbor, tempInDegree.getOrDefault(neighbor, 0) - 1);
				if (tempInDegree.get(neighbor) == 0) {
					queue.offer(neighbor);
				}
			}
		}

		if (result.size() != this.allNodes.size()) {
			List<Object> cycle = detectCycle();
			throw new CyclicOrderException("Circular ordering dependency detected", cycle);
		}

		return result;
	}

	/**
	 * Detects a cycle in the graph using Depth-First Search (DFS).
	 */
	public List<Object> detectCycle() {
		Set<Object> visited = new HashSet<>();
		Set<Object> recursionStack = new HashSet<>();
		Map<Object, Object> parent = new HashMap<>();

		for (Object node : this.allNodes) {
			if (!visited.contains(node)) {
				List<Object> cycle = dfsDetectCycle(node, visited, recursionStack, parent);
				if (!cycle.isEmpty()) {
					return cycle;
				}
			}
		}
		return Collections.emptyList();
	}

	private List<Object> dfsDetectCycle(Object node, Set<Object> visited,
			Set<Object> recursionStack, Map<Object, Object> parent) {
		visited.add(node);
		recursionStack.add(node);

		for (Object neighbor : this.adjacencyList.getOrDefault(node, Collections.emptySet())) {
			if (neighbor != null) {
				parent.put(neighbor, node);
			}

			if (!visited.contains(neighbor)) {
				List<Object> cycle = dfsDetectCycle(neighbor, visited, recursionStack, parent);
				if (!cycle.isEmpty()) {
					return cycle;
				}
			}
			else if (recursionStack.contains(neighbor)) {
				// Cycle detected - build the cycle path for the exception message
				return buildCyclePath(neighbor, node, parent);
			}
		}

		recursionStack.remove(node);
		return Collections.emptyList();
	}

	private List<Object> buildCyclePath(Object cycleStart, Object current, Map<Object, Object> parent) {
		List<Object> cycle = new ArrayList<>();
		Object node = current;

		while (node != null && !node.equals(cycleStart)) {
			cycle.add(node);
			node = parent.get(node);
		}
		if (node != null) {
			cycle.add(node);
		}

		Collections.reverse(cycle);
		return cycle;
	}
}
