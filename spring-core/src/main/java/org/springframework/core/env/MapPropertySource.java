/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.env;

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


	@Override
	public @Nullable Object getProperty(String name) {
		return this.source.get(name);
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
