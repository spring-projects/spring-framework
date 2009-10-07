package org.springframework.mapping.support;

import java.util.Stack;

import org.springframework.core.NamedThreadLocal;

class MappingContextHolder {

	private static final ThreadLocal<Stack<Object>> mappingContextHolder = new NamedThreadLocal<Stack<Object>>(
			"Mapping context");

	public static void push(Object source) {
		Stack<Object> context = getContext();
		if (context == null) {
			context = new Stack<Object>();
			mappingContextHolder.set(context);
		}
		context.add(source);
	}

	public static boolean contains(Object source) {
		return getContext().contains(source);
	}

	public static void pop() {
		Stack<Object> context = getContext();
		if (context != null) {
			context.pop();
			if (context.isEmpty()) {
				mappingContextHolder.set(null);
			}
		}
	}

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
