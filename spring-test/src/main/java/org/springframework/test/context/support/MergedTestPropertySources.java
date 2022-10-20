/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context.support;

import java.util.Arrays;

import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;

/**
 * {@code MergedTestPropertySources} encapsulates the <em>merged</em> property
 * sources declared on a test class and all of its superclasses and enclosing
 * classes via {@link TestPropertySource @TestPropertySource}.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see TestPropertySource
 */
class MergedTestPropertySources {

	private static final MergedTestPropertySources empty = new MergedTestPropertySources(new String[0], new String[0]);

	private final String[] locations;

	private final String[] properties;


	/**
	 * Factory for an <em>empty</em> {@code MergedTestPropertySources} instance.
	 */
	static MergedTestPropertySources empty() {
		return empty;
	}


	/**
	 * Create a {@code MergedTestPropertySources} instance with the supplied
	 * {@code locations} and {@code properties}.
	 * @param locations the resource locations of properties files; may be
	 * empty but never {@code null}
	 * @param properties the properties in the form of {@code key=value} pairs;
	 * may be empty but never {@code null}
	 */
	MergedTestPropertySources(String[] locations, String[] properties) {
		Assert.notNull(locations, "The locations array must not be null");
		Assert.notNull(properties, "The properties array must not be null");
		this.locations = locations;
		this.properties = properties;
	}

	/**
	 * Get the resource locations of properties files.
	 * @see TestPropertySource#locations()
	 */
	String[] getLocations() {
		return this.locations;
	}

	/**
	 * Get the properties in the form of <em>key-value</em> pairs.
	 * @see TestPropertySource#properties()
	 */
	String[] getProperties() {
		return this.properties;
	}

	/**
	 * Determine if the supplied object is equal to this {@code MergedTestPropertySources}
	 * instance by comparing both object's {@linkplain #getLocations() locations}
	 * and {@linkplain #getProperties() properties}.
	 * @since 5.3
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}

		MergedTestPropertySources that = (MergedTestPropertySources) other;
		if (!Arrays.equals(this.locations, that.locations)) {
			return false;
		}
		if (!Arrays.equals(this.properties, that.properties)) {
			return false;
		}

		return true;
	}

	/**
	 * Generate a unique hash code for all properties of this
	 * {@code MergedTestPropertySources} instance.
	 * @since 5.3
	 */
	@Override
	public int hashCode() {
		int result = Arrays.hashCode(this.locations);
		result = 31 * result + Arrays.hashCode(this.properties);
		return result;
	}

	/**
	 * Provide a String representation of this {@code MergedTestPropertySources}
	 * instance.
	 * @since 5.3
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("locations", Arrays.toString(this.locations))
				.append("properties", Arrays.toString(this.properties))
				.toString();
	}

}
