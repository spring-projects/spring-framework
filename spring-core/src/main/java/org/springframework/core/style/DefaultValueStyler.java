/*
 * Copyright 2002-2021 the original author or authors.
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
			return NULL;
		}
		else if (value instanceof String) {
			return "\'" + value + "\'";
		}
		else if (value instanceof Class) {
			return ClassUtils.getShortName((Class<?>) value);
		}
		else if (value instanceof Method method) {
			return method.getName() + "@" + ClassUtils.getShortName(method.getDeclaringClass());
		}
		else if (value instanceof Map) {
			return style((Map<?, ?>) value);
		}
		else if (value instanceof Map.Entry) {
			return style((Map.Entry<? ,?>) value);
		}
		else if (value instanceof Collection) {
			return style((Collection<?>) value);
		}
		else if (value.getClass().isArray()) {
			return styleArray(ObjectUtils.toObjectArray(value));
		}
		else {
			return String.valueOf(value);
		}
	}

	private <K, V> String style(Map<K, V> value) {
		if (value.isEmpty()) {
			return EMPTY_MAP;
		}

		StringJoiner result = new StringJoiner(", ", "[", "]");
		for (Map.Entry<K, V> entry : value.entrySet()) {
			result.add(style(entry));
		}
		return MAP + result;
	}

	private String style(Map.Entry<?, ?> value) {
		return style(value.getKey()) + " -> " + style(value.getValue());
	}

	private String style(Collection<?> value) {
		String collectionType = getCollectionTypeString(value);

		if (value.isEmpty()) {
			return collectionType + EMPTY;
		}

		StringJoiner result = new StringJoiner(", ", "[", "]");
		for (Object o : value) {
			result.add(style(o));
		}
		return collectionType + result;
	}

	private String getCollectionTypeString(Collection<?> value) {
		if (value instanceof List) {
			return LIST;
		}
		else if (value instanceof Set) {
			return SET;
		}
		else {
			return COLLECTION;
		}
	}

	private String styleArray(Object[] array) {
		if (array.length == 0) {
			return ARRAY + '<' + ClassUtils.getShortName(array.getClass().getComponentType()) + '>' + EMPTY;
		}

		StringJoiner result = new StringJoiner(", ", "[", "]");
		for (Object o : array) {
			result.add(style(o));
		}
		return ARRAY + '<' + ClassUtils.getShortName(array.getClass().getComponentType()) + '>' + result;
	}

}
