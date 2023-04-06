/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core.style;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link ValueStyler} that converts objects to String form &mdash; generally for
 * debugging purposes &mdash; using simple styling conventions that mimic the
 * {@code toString()} styling conventions for standard JDK implementations of
 * collections, maps, and arrays.
 *
 * <p>Uses the reflective visitor pattern underneath the hood to nicely
 * encapsulate styling algorithms for each type of styled object.
 *
 * <p>Favor {@link SimpleValueStyler} over {@link DefaultValueStyler} when you
 * wish to use styling similar to the JDK or when you need configurable control
 * over the styling of classes and methods.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public class SimpleValueStyler extends DefaultValueStyler {

	/**
	 * Default {@link Class} styling function: {@link Class#getCanonicalName()}.
	 */
	public static final Function<Class<?>, String> DEFAULT_CLASS_STYLER = Class::getCanonicalName;

	/**
	 * Default {@link Method} styling function: converts the supplied {@link Method}
	 * to a simple string representation of the method's signature in the form of
	 * {@code <method name>(<parameter types>)}, where {@code <parameter types>}
	 * is a comma-separated list of the {@linkplain Class#getSimpleName() simple names}
	 * of the parameter types.
	 * <p>For example, if the supplied method is a reference to
	 * {@link String#getBytes(java.nio.charset.Charset)}, this function will
	 * return {@code "getBytes(Charset)"}.
	 */
	public static final Function<Method, String> DEFAULT_METHOD_STYLER = SimpleValueStyler::toSimpleMethodSignature;


	private final Function<Class<?>, String> classStyler;

	private final Function<Method, String> methodStyler;


	/**
	 * Create a {@code SimpleValueStyler} using the {@link #DEFAULT_CLASS_STYLER}
	 * and {@link #DEFAULT_METHOD_STYLER}.
	 */
	public SimpleValueStyler() {
		this(DEFAULT_CLASS_STYLER, DEFAULT_METHOD_STYLER);
	}

	/**
	 * Create a {@code SimpleValueStyler} using the supplied class and method stylers.
	 * @param classStyler a function that applies styling to a {@link Class}
	 * @param methodStyler a function that applies styling to a {@link Method}
	 */
	public SimpleValueStyler(Function<Class<?>, String> classStyler, Function<Method, String> methodStyler) {
		this.classStyler = classStyler;
		this.methodStyler = methodStyler;
	}


	@Override
	protected String styleNull() {
		return "null";
	}

	@Override
	protected String styleString(String str) {
		return "\"" + str + "\"";
	}

	@Override
	protected String styleClass(Class<?> clazz) {
		return this.classStyler.apply(clazz);
	}

	@Override
	protected String styleMethod(Method method) {
		return this.methodStyler.apply(method);
	}

	@Override
	protected <K, V> String styleMap(Map<K, V> map) {
		StringJoiner result = new StringJoiner(", ", "{", "}");
		for (Map.Entry<K, V> entry : map.entrySet()) {
			result.add(style(entry));
		}
		return result.toString();
	}

	@Override
	protected String styleCollection(Collection<?> collection) {
		StringJoiner result = new StringJoiner(", ", "[", "]");
		for (Object element : collection) {
			result.add(style(element));
		}
		return result.toString();
	}

	@Override
	protected String styleArray(Object[] array) {
		StringJoiner result = new StringJoiner(", ", "[", "]");
		for (Object element : array) {
			result.add(style(element));
		}
		return result.toString();
	}

	private static String toSimpleMethodSignature(Method method) {
		String parameterList = Arrays.stream(method.getParameterTypes())
				.map(Class::getSimpleName)
				.collect(Collectors.joining(", "));
		return String.format("%s(%s)", method.getName(), parameterList);
	}

}
