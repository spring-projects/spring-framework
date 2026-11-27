/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * @author Nabil Fawwaz Elqayyim
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
			}
			else {
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
