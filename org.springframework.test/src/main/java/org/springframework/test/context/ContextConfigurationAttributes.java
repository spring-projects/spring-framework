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

package org.springframework.test.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * <code>ContextConfigurationAttributes</code> encapsulates the context 
 * configuration attributes declared on a test class via
 * {@link ContextConfiguration @ContextConfiguration}.
 * 
 * @author Sam Brannen
 * @since 3.1
 * @see ContextConfiguration
 * @see SmartContextLoader#processContextConfiguration(ContextConfigurationAttributes)
 * @see MergedContextConfiguration
 */
public class ContextConfigurationAttributes {

	private static final Log logger = LogFactory.getLog(ContextConfigurationAttributes.class);

	private final Class<?> declaringClass;

	private String[] locations;

	private Class<?>[] classes;

	private final boolean inheritLocations;

	private final Class<? extends ContextLoader> contextLoaderClass;


	/**
	 * Resolve resource locations from the {@link ContextConfiguration#locations() locations}
	 * and {@link ContextConfiguration#value() value} attributes of the supplied
	 * {@link ContextConfiguration} annotation.
	 * 
	 * @throws IllegalStateException if both the locations and value attributes have been declared
	 */
	static String[] resolveLocations(Class<?> declaringClass, ContextConfiguration contextConfiguration) {
		Assert.notNull(declaringClass, "declaringClass must not be null");

		String[] locations = contextConfiguration.locations();
		String[] valueLocations = contextConfiguration.value();

		if (!ObjectUtils.isEmpty(valueLocations) && !ObjectUtils.isEmpty(locations)) {
			String msg = String.format("Test class [%s] has been configured with @ContextConfiguration's 'value' %s "
					+ "and 'locations' %s attributes. Only one declaration of resource "
					+ "locations is permitted per @ContextConfiguration annotation.", declaringClass.getName(),
				ObjectUtils.nullSafeToString(valueLocations), ObjectUtils.nullSafeToString(locations));
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
		else if (!ObjectUtils.isEmpty(valueLocations)) {
			locations = valueLocations;
		}

		return locations;
	}

	/**
	 * Construct a new {@link ContextConfigurationAttributes} instance for the
	 * supplied {@link ContextConfiguration @ContextConfiguration} annotation and
	 * the {@link Class test class} that declared it.
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @param contextConfiguration the annotation from which to retrieve the attributes 
	 */
	public ContextConfigurationAttributes(Class<?> declaringClass, ContextConfiguration contextConfiguration) {
		this(declaringClass, resolveLocations(declaringClass, contextConfiguration), contextConfiguration.classes(),
			contextConfiguration.inheritLocations(), contextConfiguration.loader());
	}

	/**
	 * Construct a new {@link ContextConfigurationAttributes} instance for the
	 * {@link Class test class} that declared the
	 * {@link ContextConfiguration @ContextConfiguration} annotation and its
	 * corresponding attributes.
	 * 
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @param locations the resource locations declared via {@code @ContextConfiguration}
	 * @param classes the configuration classes declared via {@code @ContextConfiguration}
	 * @param inheritLocations the <code>inheritLocations</code> flag declared via {@code @ContextConfiguration}
	 * @param contextLoaderClass the {@code ContextLoader} class declared via {@code @ContextConfiguration}
	 */
	public ContextConfigurationAttributes(Class<?> declaringClass, String[] locations, Class<?>[] classes,
			boolean inheritLocations, Class<? extends ContextLoader> contextLoaderClass) {
		this.declaringClass = declaringClass;
		this.locations = locations;
		this.classes = classes;
		this.inheritLocations = inheritLocations;
		this.contextLoaderClass = contextLoaderClass;
	}

	/**
	 * Get the {@link Class class} that declared the
	 * {@link ContextConfiguration @ContextConfiguration} annotation.
	 * @return the declaring class; never <code>null</code>
	 */
	public Class<?> getDeclaringClass() {
		return this.declaringClass;
	}

	/**
	 * Get the resource locations that were declared via
	 * {@link ContextConfiguration @ContextConfiguration}.
	 * <p>Note: this is a mutable property. The returned value may therefore
	 * represent a <em>processed</em> value that does not match the original value 
	 * declared via {@link ContextConfiguration @ContextConfiguration}.
	 * @return the resource locations; potentially <code>null</code> or <em>empty</em>
	 * @see ContextConfiguration#value
	 * @see ContextConfiguration#locations
	 * @see #setLocations()
	 */
	public String[] getLocations() {
		return this.locations;
	}

	/**
	 * Set the <em>processed</em> resource locations, effectively overriding the
	 * original value declared via {@link ContextConfiguration @ContextConfiguration}.
	 * @see #getLocations()
	 */
	public void setLocations(String[] locations) {
		this.locations = locations;
	}

	/**
	 * Get the configuration classes that were declared via
	 * {@link ContextConfiguration @ContextConfiguration}.
	 * <p>Note: this is a mutable property. The returned value may therefore
	 * represent a <em>processed</em> value that does not match the original value 
	 * declared via {@link ContextConfiguration @ContextConfiguration}.
	 * @return the configuration classes; potentially <code>null</code> or <em>empty</em>
	 * @see ContextConfiguration#classes
	 * @see #setClasses()
	 */
	public Class<?>[] getClasses() {
		return this.classes;
	}

	/**
	 * Set the <em>processed</em> configuration classes, effectively overriding the
	 * original value declared via {@link ContextConfiguration @ContextConfiguration}.
	 * @see #getClasses()
	 */
	public void setClasses(Class<?>[] classes) {
		this.classes = classes;
	}

	/**
	 * Get the <code>inheritLocations</code> flag that was declared via
	 * {@link ContextConfiguration @ContextConfiguration}.
	 * @return the <code>inheritLocations</code> flag
	 * @see ContextConfiguration#inheritLocations
	 */
	public boolean isInheritLocations() {
		return this.inheritLocations;
	}

	/**
	 * Get the <code>ContextLoader</code> class that was declared via
	 * {@link ContextConfiguration @ContextConfiguration}.
	 * @return the <code>ContextLoader</code> class
	 * @see ContextConfiguration#loader
	 */
	public Class<? extends ContextLoader> getContextLoaderClass() {
		return this.contextLoaderClass;
	}

	/**
	 * Provide a String representation of the context configuration attributes
	 * and declaring class.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)//
		.append("declaringClass", this.declaringClass)//
		.append("locations", ObjectUtils.nullSafeToString(this.locations))//
		.append("classes", ObjectUtils.nullSafeToString(this.classes))//
		.append("inheritLocations", this.inheritLocations)//
		.append("contextLoaderClass", this.contextLoaderClass)//
		.toString();
	}

}
