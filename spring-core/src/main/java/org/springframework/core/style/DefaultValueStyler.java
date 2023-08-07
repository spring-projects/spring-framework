/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Converts objects to String form, generally for debugging purposes,
 * using Spring's {@code toString} styling conventions.
 *
 * <p>Uses the reflective visitor pattern underneath the hood to nicely
 * encapsulate styling algorithms for each type of styled object.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.2.2
 */
public class DefaultValueStyler implements ValueStyler {

	private static final String EMPTY = "[[empty]]";
	private static final String NULL = "[null]";
	private static final String COLLECTION = "collection";
	private static final String SET = "set";
	private static final String LIST = "list";
	private static final String MAP = "map";
	private static final String EMPTY_MAP = MAP + EMPTY;
	private static final String ARRAY = "array";


	@Override
	public String style(@Nullable Object value) {
		if (value == null) {
			return styleNull();
		}
		else if (value instanceof String str) {
			return styleString(str);
		}
		else if (value instanceof Class<?> clazz) {
			return styleClass(clazz);
		}
		else if (value instanceof Method method) {
			return styleMethod(method);
		}
		else if (value instanceof Map<?, ?> map) {
			return styleMap(map);
		}
		else if (value instanceof Map.Entry<?, ?> entry) {
			return styleMapEntry(entry);
		}
		else if (value instanceof Collection<?> collection) {
			return styleCollection(collection);
		}
		else if (value.getClass().isArray()) {
			return styleArray(ObjectUtils.toObjectArray(value));
		}
		else {
			return styleObject(value);
		}
	}

	/**
	 * Generate a styled version of {@code null}.
	 * <p>The default implementation returns {@code "[null]"}.
	 * @return a styled version of {@code null}
	 * @since 6.0
	 */
	protected String styleNull() {
		return NULL;
	}

	/**
	 * Generate a styled version of the supplied {@link String}.
	 * <p>The default implementation returns the supplied string wrapped in
	 * single quotes.
	 * @return a styled version of the supplied string
	 * @since 6.0
	 */
	protected String styleString(String str) {
		return "\'" + str + "\'";
	}

	/**
	 * Generate a styled version of the supplied {@link Class}.
	 * <p>The default implementation delegates to {@link ClassUtils#getShortName(Class)}.
	 * @return a styled version of the supplied class
	 * @since 6.0
	 */
	protected String styleClass(Class<?> clazz) {
		return ClassUtils.getShortName(clazz);
	}

	/**
	 * Generate a styled version of the supplied {@link Method}.
	 * <p>The default implementation returns the method's {@linkplain Method#getName()
	 * name} and the {@linkplain ClassUtils#getShortName(Class) short name} of the
	 * method's {@linkplain Method#getDeclaringClass() declaring class}, separated by
	 * the {@code "@"} symbol.
	 * @return a styled version of the supplied method
	 * @since 6.0
	 */
	protected String styleMethod(Method method) {
		return method.getName() + "@" + ClassUtils.getShortName(method.getDeclaringClass());
	}

	/**
	 * Generate a styled version of the supplied {@link Map}.
	 * @return a styled version of the supplied map
	 * @since 6.0
	 */
	protected <K, V> String styleMap(Map<K, V> map) {
		if (map.isEmpty()) {
			return EMPTY_MAP;
		}

		StringJoiner result = new StringJoiner(", ", "[", "]");
		for (Map.Entry<K, V> entry : map.entrySet()) {
			result.add(styleMapEntry(entry));
		}
		return MAP + result;
	}

	/**
	 * Generate a styled version of the supplied {@link Map.Entry}.
	 * @return a styled version of the supplied map entry
	 * @since 6.0
	 */
	protected String styleMapEntry(Map.Entry<?, ?> entry) {
		return style(entry.getKey()) + " -> " + style(entry.getValue());
	}

	/**
	 * Generate a styled version of the supplied {@link Collection}.
	 * @return a styled version of the supplied collection
	 * @since 6.0
	 */
	protected String styleCollection(Collection<?> collection) {
		String collectionType = getCollectionTypeString(collection);

		if (collection.isEmpty()) {
			return collectionType + EMPTY;
		}

		StringJoiner result = new StringJoiner(", ", "[", "]");
		for (Object element : collection) {
			result.add(style(element));
		}
		return collectionType + result;
	}

	/**
	 * Generate a styled version of the supplied array.
	 * @return a styled version of the supplied array
	 * @since 6.0
	 */
	protected String styleArray(Object[] array) {
		if (array.length == 0) {
			return ARRAY + '<' + ClassUtils.getShortName(array.getClass().componentType()) + '>' + EMPTY;
		}

		StringJoiner result = new StringJoiner(", ", "[", "]");
		for (Object element : array) {
			result.add(style(element));
		}
		return ARRAY + '<' + ClassUtils.getShortName(array.getClass().componentType()) + '>' + result;
	}

	/**
	 * Generate a styled version of the supplied {@link Object}.
	 * <p>This method is only invoked by {@link #style(Object)} as a fallback,
	 * if none of the other {@code style*()} methods is suitable for the object's
	 * type.
	 * <p>The default implementation delegates to {@link String#valueOf(Object)}.
	 * @return a styled version of the supplied object
	 * @since 6.0
	 */
	protected String styleObject(Object obj) {
		return String.valueOf(obj);
	}


	private static String getCollectionTypeString(Collection<?> collection) {
		if (collection instanceof List) {
			return LIST;
		}
		else if (collection instanceof Set) {
			return SET;
		}
		else {
			return COLLECTION;
		}
	}

}
