/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.List;

import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@code TestPropertySourceAttributes} encapsulates attributes declared
 * via {@link TestPropertySource @TestPropertySource} annotations.
 *
 * <p>In addition to encapsulating declared attributes,
 * {@code TestPropertySourceAttributes} also enforces configuration rules.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see TestPropertySource
 * @see MergedTestPropertySources
 */
class TestPropertySourceAttributes {

	private final Class<?> declaringClass;

	private final String[] locations;

	private final boolean inheritLocations;

	private final String[] properties;

	private final boolean inheritProperties;


	/**
	 * Create a new {@code TestPropertySourceAttributes} instance for the supplied
	 * values and enforce configuration rules.
	 * @param declaringClass the class that declared {@code @TestPropertySource}
	 * @param locations the merged {@link TestPropertySource#locations()}
	 * @param inheritLocations the {@link TestPropertySource#inheritLocations()} flag
	 * @param properties the merged {@link TestPropertySource#properties()}
	 * @param inheritProperties the {@link TestPropertySource#inheritProperties()} flag
	 * @since 5.2
	 */
	TestPropertySourceAttributes(Class<?> declaringClass, List<String> locations, boolean inheritLocations,
			List<String> properties, boolean inheritProperties) {

		this(declaringClass, locations.toArray(new String[0]), inheritLocations, properties.toArray(new String[0]),
			inheritProperties);
	}

	private TestPropertySourceAttributes(Class<?> declaringClass, String[] locations, boolean inheritLocations,
			String[] properties, boolean inheritProperties) {

		Assert.notNull(declaringClass, "'declaringClass' must not be null");
		Assert.isTrue(!ObjectUtils.isEmpty(locations) || !ObjectUtils.isEmpty(properties),
			"Either 'locations' or 'properties' are required");

		this.declaringClass = declaringClass;
		this.locations = locations;
		this.inheritLocations = inheritLocations;
		this.properties = properties;
		this.inheritProperties = inheritProperties;
	}


	/**
	 * Get the {@linkplain Class class} that declared {@code @TestPropertySource}.
	 * @return the declaring class; never {@code null}
	 */
	Class<?> getDeclaringClass() {
		return this.declaringClass;
	}

	/**
	 * Get the resource locations that were declared via {@code @TestPropertySource}.
	 * <p>Note: The returned value may represent a <em>detected default</em>
	 * or merged locations that do not match the original value declared via a
	 * single {@code @TestPropertySource} annotation.
	 * @return the resource locations; potentially <em>empty</em>
	 * @see TestPropertySource#value
	 * @see TestPropertySource#locations
	 */
	String[] getLocations() {
		return this.locations;
	}

	/**
	 * Get the {@code inheritLocations} flag that was declared via {@code @TestPropertySource}.
	 * @return the {@code inheritLocations} flag
	 * @see TestPropertySource#inheritLocations
	 */
	boolean isInheritLocations() {
		return this.inheritLocations;
	}

	/**
	 * Get the inlined properties that were declared via {@code @TestPropertySource}.
	 * <p>Note: The returned value may represent merged properties that do not
	 * match the original value declared via a single {@code @TestPropertySource}
	 * annotation.
	 * @return the inlined properties; potentially <em>empty</em>
	 * @see TestPropertySource#properties
	 */
	String[] getProperties() {
		return this.properties;
	}

	/**
	 * Get the {@code inheritProperties} flag that was declared via {@code @TestPropertySource}.
	 * @return the {@code inheritProperties} flag
	 * @see TestPropertySource#inheritProperties
	 */
	boolean isInheritProperties() {
		return this.inheritProperties;
	}

	/**
	 * Provide a String representation of the {@code @TestPropertySource}
	 * attributes and declaring class.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)//
		.append("declaringClass", this.declaringClass.getName())//
		.append("locations", ObjectUtils.nullSafeToString(this.locations))//
		.append("inheritLocations", this.inheritLocations)//
		.append("properties", ObjectUtils.nullSafeToString(this.properties))//
		.append("inheritProperties", this.inheritProperties)//
		.toString();
	}

}
