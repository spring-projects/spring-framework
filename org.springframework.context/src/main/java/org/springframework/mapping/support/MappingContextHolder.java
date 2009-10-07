/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.mapping.support;

import java.util.Stack;

import org.springframework.core.NamedThreadLocal;

/**
 * Holds thread-specific context about a mapping operation
 * @author Keith Donald
 * @see SpelMapper#map(Object, Object)
 */
class MappingContextHolder {

	private static final ThreadLocal<Stack<Object>> mappingContextHolder = new NamedThreadLocal<Stack<Object>>(
			"Mapping context");

	/**
	 * Push a source object being mapped onto the context.
	 */
	public static void push(Object source) {
		Stack<Object> context = getContext();
		if (context == null) {
			context = new Stack<Object>();
			mappingContextHolder.set(context);
		}
		context.add(source);
	}

	/**
	 * Is the source being mapped or has already been mapped?
	 */
	public static boolean contains(Object source) {
		return getContext().contains(source);
	}

	/**
	 * Pop the source object being mapped off the context; mapping is complete.
	 */
	public static void pop() {
		Stack<Object> context = getContext();
		if (context != null) {
			context.pop();
			if (context.isEmpty()) {
				mappingContextHolder.set(null);
			}
		}
	}

	/**
	 * Return a level of bullets for indenting mapping debug logs based on the depth in the object graph.
	 */
	public static String getLevel() {
		int size = getContext().size();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < size; i++) {
			builder.append("*");
		}
		builder.append(" ");
		return builder.toString();
	}

	private static Stack<Object> getContext() {
		return mappingContextHolder.get();
	}

}
