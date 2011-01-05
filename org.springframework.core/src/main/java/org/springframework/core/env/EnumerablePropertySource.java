/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.core.env;

import org.springframework.util.Assert;

/**
 * TODO SPR-7508: document
 *
 * @author Chris Beams
 * @since 3.1
 */
public abstract class EnumerablePropertySource<T> extends PropertySource<T> {

	protected static final String[] EMPTY_NAMES_ARRAY = new String[0];


	public EnumerablePropertySource(String name, T source) {
		super(name, source);
	}

	/**
	 * Return the names of all properties contained by the {@linkplain #getSource()
	 * source} object (never {@code null}).
	 */
	public abstract String[] getPropertyNames();

	/**
	 * Return whether this {@code PropertySource} contains the given key.
	 * <p>This implementation checks for the presence of the given key within
	 * the {@link #getPropertyNames()} array.
	 * @param key the property key to find
	 */
	public boolean containsProperty(String name) {
		Assert.notNull(name, "property name must not be null");
		for (String candidate : this.getPropertyNames()) {
			if (candidate.equals(name)) {
				return true;
			}
		}
		return false;
	}

}
