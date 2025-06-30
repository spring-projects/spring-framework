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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Property editor for Maps, converting any source Map
 * to a given target Map type.
 *
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see java.util.Map
 * @see java.util.SortedMap
 */
public class CustomMapEditor extends PropertyEditorSupport {

	@SuppressWarnings("rawtypes")
	private final Class<? extends Map> mapType;

	private final boolean nullAsEmptyMap;


	/**
	 * Create a new CustomMapEditor for the given target type,
	 * keeping an incoming {@code null} as-is.
	 * @param mapType the target type, which needs to be a
	 * sub-interface of Map or a concrete Map class
	 * @see java.util.Map
	 * @see java.util.HashMap
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings("rawtypes")
	public CustomMapEditor(Class<? extends Map> mapType) {
		this(mapType, false);
	}

	/**
	 * Create a new CustomMapEditor for the given target type.
	 * <p>If the incoming value is of the given type, it will be used as-is.
	 * If it is a different Map type or an array, it will be converted
	 * to a default implementation of the given Map type.
	 * If the value is anything else, a target Map with that single
	 * value will be created.
	 * <p>The default Map implementations are: TreeMap for SortedMap,
	 * and LinkedHashMap for Map.
	 * @param mapType the target type, which needs to be a
	 * sub-interface of Map or a concrete Map class
	 * @param nullAsEmptyMap ap whether to convert an incoming {@code null}
	 * value to an empty Map (of the appropriate type)
	 * @see java.util.Map
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings("rawtypes")
	public CustomMapEditor(Class<? extends Map> mapType, boolean nullAsEmptyMap) {
		Assert.notNull(mapType, "Map type is required");
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException(
					"Map type [" + mapType.getName() + "] does not implement [java.util.Map]");
		}
		this.mapType = mapType;
		this.nullAsEmptyMap = nullAsEmptyMap;
	}


	/**
	 * Convert the given text value to a Map with a single element.
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(text);
	}

	/**
	 * Convert the given value to a Map of the target type.
	 */
	@Override
	public void setValue(@Nullable Object value) {
		if (value == null && this.nullAsEmptyMap) {
			super.setValue(createMap(this.mapType, 0));
		}
		else if (value == null || (this.mapType.isInstance(value) && !alwaysCreateNewMap())) {
			// Use the source value as-is, as it matches the target type.
			super.setValue(value);
		}
		else if (value instanceof Map<?, ?> source) {
			// Convert Map elements.
			Map<Object, Object> target = createMap(this.mapType, source.size());
			source.forEach((key, val) -> target.put(convertKey(key), convertValue(val)));
			super.setValue(target);
		}
		else {
			throw new IllegalArgumentException("Value cannot be converted to Map: " + value);
		}
	}

	/**
	 * Create a Map of the given type, with the given
	 * initial capacity (if supported by the Map type).
	 * @param mapType a sub-interface of Map
	 * @param initialCapacity the initial capacity
	 * @return the new Map instance
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected Map<Object, Object> createMap(Class<? extends Map> mapType, int initialCapacity) {
		if (!mapType.isInterface()) {
			try {
				return ReflectionUtils.accessibleConstructor(mapType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not instantiate map class: " + mapType.getName(), ex);
			}
		}
		else if (SortedMap.class == mapType) {
			return new TreeMap<>();
		}
		else {
			return new LinkedHashMap<>(initialCapacity);
		}
	}

	/**
	 * Return whether to always create a new Map,
	 * even if the type of the passed-in Map already matches.
	 * <p>Default is "false"; can be overridden to enforce creation of a
	 * new Map, for example to convert elements in any case.
	 * @see #convertKey
	 * @see #convertValue
	 */
	protected boolean alwaysCreateNewMap() {
		return false;
	}

	/**
	 * Hook to convert each encountered Map key.
	 * The default implementation simply returns the passed-in key as-is.
	 * <p>Can be overridden to perform conversion of certain keys,
	 * for example from String to Integer.
	 * <p>Only called if actually creating a new Map!
	 * This is by default not the case if the type of the passed-in Map
	 * already matches. Override {@link #alwaysCreateNewMap()} to
	 * enforce creating a new Map in every case.
	 * @param key the source key
	 * @return the key to be used in the target Map
	 * @see #alwaysCreateNewMap
	 */
	protected Object convertKey(Object key) {
		return key;
	}

	/**
	 * Hook to convert each encountered Map value.
	 * The default implementation simply returns the passed-in value as-is.
	 * <p>Can be overridden to perform conversion of certain values,
	 * for example from String to Integer.
	 * <p>Only called if actually creating a new Map!
	 * This is by default not the case if the type of the passed-in Map
	 * already matches. Override {@link #alwaysCreateNewMap()} to
	 * enforce creating a new Map in every case.
	 * @param value the source value
	 * @return the value to be used in the target Map
	 * @see #alwaysCreateNewMap
	 */
	protected Object convertValue(Object value) {
		return value;
	}


	/**
	 * This implementation returns {@code null} to indicate that
	 * there is no appropriate text representation.
	 */
	@Override
	public @Nullable String getAsText() {
		return null;
	}

}
