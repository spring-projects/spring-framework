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

package org.springframework.core.env;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * {@link PropertySource} that reads keys and values from a {@code Map} object.
 * The underlying map should not contain any {@code null} values in order to
 * comply with {@link #getProperty} and {@link #containsProperty} semantics.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertiesPropertySource
 */
public class MapPropertySource extends EnumerablePropertySource<Map<String, Object>> {

	/**
	 * Create a new {@code MapPropertySource} with the given name and {@code Map}.
	 * @param name the associated name
	 * @param source the Map source (without {@code null} values in order to get
	 * consistent {@link #getProperty} and {@link #containsProperty} behavior)
	 */
	public MapPropertySource(String name, Map<String, Object> source) {
		super(name, source);
	}

	/**
	 * Returns the value associated with the given property name.
	 * <p>
	 * First, this method checks for an exact match in the underlying {@code source} map.
	 * If not found, it attempts to reconstruct a {@link List} from sequentially indexed keys
	 * (e.g. {@code name[0]}, {@code name[1]}, ...), stopping at the first missing index.
	 * <p>
	 * Values that implement {@link CharSequence} are converted to plain {@link String} instances.
	 *
	 * @param name the property name to resolve
	 * @return the resolved value, or {@code null} if not found
	 */
	@Override
	public @Nullable Object getProperty(String name) {
		Object directMatch = this.source.get(name);

		if (directMatch != null) {
			return directMatch;
		}

		List<Object> collectedValues = new ArrayList<>();
		for (int index = 0; ; index++) {
			String indexedKey = name + "[" + index + "]";
			if (!this.source.containsKey(indexedKey)) {
				break;
			}

			Object rawIndexedValue = this.source.get(indexedKey);

			if (rawIndexedValue instanceof CharSequence cs) {
				collectedValues.add(cs.toString());
			} else {
				collectedValues.add(rawIndexedValue);
			}
		}

		return collectedValues.isEmpty() ? null : collectedValues;
	}

	@Override
	public boolean containsProperty(String name) {
		return this.source.containsKey(name);
	}

	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.source.keySet());
	}

}
