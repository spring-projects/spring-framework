/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@code ContextConfigurationAttributes} encapsulates the context
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

	private final Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] initializers;

	private final boolean inheritInitializers;

	private final String name;


	/**
	 * Resolve resource locations from the {@link ContextConfiguration#locations() locations}
	 * and {@link ContextConfiguration#value() value} attributes of the supplied
	 * {@link ContextConfiguration} annotation.
	 *
	 * @throws IllegalStateException if both the locations and value attributes have been declared
	 */
	private static String[] resolveLocations(Class<?> declaringClass, ContextConfiguration contextConfiguration) {
		return resolveLocations(declaringClass, contextConfiguration.locations(), contextConfiguration.value());
	}

	/**
	 * Resolve resource locations from the supplied {@code locations} and
	 * {@code value} arrays, which correspond to attributes of the same names in
	 * the {@link ContextConfiguration} annotation.
	 *
	 * @throws IllegalStateException if both the locations and value attributes have been declared
	 */
	private static String[] resolveLocations(Class<?> declaringClass, String[] locations, String[] value) {
		Assert.notNull(declaringClass, "declaringClass must not be null");

		if (!ObjectUtils.isEmpty(value) && !ObjectUtils.isEmpty(locations)) {
			String msg = String.format("Test class [%s] has been configured with @ContextConfiguration's 'value' %s "
					+ "and 'locations' %s attributes. Only one declaration of resource "
					+ "locations is permitted per @ContextConfiguration annotation.", declaringClass.getName(),
				ObjectUtils.nullSafeToString(value), ObjectUtils.nullSafeToString(locations));
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
		else if (!ObjectUtils.isEmpty(value)) {
			locations = value;
		}

		return locations;
	}

	/**
	 * Construct a new {@link ContextConfigurationAttributes} instance for the
	 * supplied {@link ContextConfiguration @ContextConfiguration} annotation and
	 * the {@linkplain Class test class} that declared it.
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @param contextConfiguration the annotation from which to retrieve the attributes
	 */
	public ContextConfigurationAttributes(Class<?> declaringClass, ContextConfiguration contextConfiguration) {
		this(declaringClass, resolveLocations(declaringClass, contextConfiguration), contextConfiguration.classes(),
			contextConfiguration.inheritLocations(), contextConfiguration.initializers(),
			contextConfiguration.inheritInitializers(), contextConfiguration.name(), contextConfiguration.loader());
	}

	/**
	 * Construct a new {@link ContextConfigurationAttributes} instance for the
	 * supplied {@link ContextConfiguration @ContextConfiguration} annotation and
	 * the {@linkplain Class test class} that declared it.
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @param annAttrs the annotation attributes from which to retrieve the attributes
	 */
	@SuppressWarnings("unchecked")
	public ContextConfigurationAttributes(Class<?> declaringClass, AnnotationAttributes annAttrs) {
		this(
			declaringClass,
			resolveLocations(declaringClass, annAttrs.getStringArray("locations"), annAttrs.getStringArray("value")),
			annAttrs.getClassArray("classes"),
			annAttrs.getBoolean("inheritLocations"),
			(Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[]) annAttrs.getClassArray("initializers"),
			annAttrs.getBoolean("inheritInitializers"), annAttrs.getString("name"),
			(Class<? extends ContextLoader>) annAttrs.getClass("loader"));
	}

	/**
	 * Construct a new {@link ContextConfigurationAttributes} instance for the
	 * {@linkplain Class test class} that declared the
	 * {@link ContextConfiguration @ContextConfiguration} annotation and its
	 * corresponding attributes.
	 *
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @param locations the resource locations declared via {@code @ContextConfiguration}
	 * @param classes the annotated classes declared via {@code @ContextConfiguration}
	 * @param inheritLocations the {@code inheritLocations} flag declared via {@code @ContextConfiguration}
	 * @param contextLoaderClass the {@code ContextLoader} class declared via {@code @ContextConfiguration}
	 * @throws IllegalArgumentException if the {@code declaringClass} or {@code contextLoaderClass} is
	 * {@code null}, or if the {@code locations} and {@code classes} are both non-empty
	 * @deprecated as of Spring 3.2, use
	 * {@link #ContextConfigurationAttributes(Class, String[], Class[], boolean, Class[], boolean, String, Class)}
	 * instead
	 */
	@Deprecated
	public ContextConfigurationAttributes(Class<?> declaringClass, String[] locations, Class<?>[] classes,
			boolean inheritLocations, Class<? extends ContextLoader> contextLoaderClass) {
		this(declaringClass, locations, classes, inheritLocations, null, true, null, contextLoaderClass);
	}

	/**
	 * Construct a new {@link ContextConfigurationAttributes} instance for the
	 * {@linkplain Class test class} that declared the
	 * {@link ContextConfiguration @ContextConfiguration} annotation and its
	 * corresponding attributes.
	 *
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @param locations the resource locations declared via {@code @ContextConfiguration}
	 * @param classes the annotated classes declared via {@code @ContextConfiguration}
	 * @param inheritLocations the {@code inheritLocations} flag declared via {@code @ContextConfiguration}
	 * @param initializers the context initializers declared via {@code @ContextConfiguration}
	 * @param inheritInitializers the {@code inheritInitializers} flag declared via {@code @ContextConfiguration}
	 * @param contextLoaderClass the {@code ContextLoader} class declared via {@code @ContextConfiguration}
	 * @throws IllegalArgumentException if the {@code declaringClass} or {@code contextLoaderClass} is
	 * {@code null}, or if the {@code locations} and {@code classes} are both non-empty
	 */
	public ContextConfigurationAttributes(Class<?> declaringClass, String[] locations, Class<?>[] classes,
			boolean inheritLocations,
			Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] initializers,
			boolean inheritInitializers, Class<? extends ContextLoader> contextLoaderClass) {
		this(declaringClass, locations, classes, inheritLocations, initializers, inheritInitializers, null,
			contextLoaderClass);
	}

	/**
	 * Construct a new {@link ContextConfigurationAttributes} instance for the
	 * {@linkplain Class test class} that declared the
	 * {@link ContextConfiguration @ContextConfiguration} annotation and its
	 * corresponding attributes.
	 *
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @param locations the resource locations declared via {@code @ContextConfiguration}
	 * @param classes the annotated classes declared via {@code @ContextConfiguration}
	 * @param inheritLocations the {@code inheritLocations} flag declared via {@code @ContextConfiguration}
	 * @param initializers the context initializers declared via {@code @ContextConfiguration}
	 * @param inheritInitializers the {@code inheritInitializers} flag declared via {@code @ContextConfiguration}
	 * @param name the name of level in the context hierarchy, or {@code null} if not applicable
	 * @param contextLoaderClass the {@code ContextLoader} class declared via {@code @ContextConfiguration}
	 * @throws IllegalArgumentException if the {@code declaringClass} or {@code contextLoaderClass} is
	 * {@code null}, or if the {@code locations} and {@code classes} are both non-empty
	 */
	public ContextConfigurationAttributes(Class<?> declaringClass, String[] locations, Class<?>[] classes,
			boolean inheritLocations,
			Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] initializers,
			boolean inheritInitializers, String name, Class<? extends ContextLoader> contextLoaderClass) {

		Assert.notNull(declaringClass, "declaringClass must not be null");
		Assert.notNull(contextLoaderClass, "contextLoaderClass must not be null");

		if (!ObjectUtils.isEmpty(locations) && !ObjectUtils.isEmpty(classes)) {
			String msg = String.format(
				"Test class [%s] has been configured with @ContextConfiguration's 'locations' (or 'value') %s "
						+ "and 'classes' %s attributes. Only one declaration of resources "
						+ "is permitted per @ContextConfiguration annotation.", declaringClass.getName(),
				ObjectUtils.nullSafeToString(locations), ObjectUtils.nullSafeToString(classes));
			logger.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.declaringClass = declaringClass;
		this.locations = locations;
		this.classes = classes;
		this.inheritLocations = inheritLocations;
		this.initializers = initializers;
		this.inheritInitializers = inheritInitializers;
		this.name = StringUtils.hasText(name) ? name : null;
		this.contextLoaderClass = contextLoaderClass;
	}

	/**
	 * Get the {@linkplain Class class} that declared the
	 * {@link ContextConfiguration @ContextConfiguration} annotation.
	 *
	 * @return the declaring class; never {@code null}
	 */
	public Class<?> getDeclaringClass() {
		return declaringClass;
	}

	/**
	 * Get the resource locations that were declared via
	 * {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * <p>Note: this is a mutable property. The returned value may therefore
	 * represent a <em>processed</em> value that does not match the original value
	 * declared via {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * @return the resource locations; potentially {@code null} or <em>empty</em>
	 * @see ContextConfiguration#value
	 * @see ContextConfiguration#locations
	 * @see #setLocations(String[])
	 */
	public String[] getLocations() {
		return locations;
	}

	/**
	 * Set the <em>processed</em> resource locations, effectively overriding the
	 * original value declared via {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * @see #getLocations()
	 */
	public void setLocations(String[] locations) {
		this.locations = locations;
	}

	/**
	 * Get the annotated classes that were declared via
	 * {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * <p>Note: this is a mutable property. The returned value may therefore
	 * represent a <em>processed</em> value that does not match the original value
	 * declared via {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * @return the annotated classes; potentially {@code null} or <em>empty</em>
	 * @see ContextConfiguration#classes
	 * @see #setClasses(Class[])
	 */
	public Class<?>[] getClasses() {
		return classes;
	}

	/**
	 * Set the <em>processed</em> annotated classes, effectively overriding the
	 * original value declared via {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * @see #getClasses()
	 */
	public void setClasses(Class<?>[] classes) {
		this.classes = classes;
	}

	/**
	 * Determine if this {@code ContextConfigurationAttributes} instance has
	 * path-based resource locations.
	 *
	 * @return {@code true} if the {@link #getLocations() locations} array is not empty
	 * @see #hasResources()
	 * @see #hasClasses()
	 */
	public boolean hasLocations() {
		return !ObjectUtils.isEmpty(getLocations());
	}

	/**
	 * Determine if this {@code ContextConfigurationAttributes} instance has
	 * class-based resources.
	 *
	 * @return {@code true} if the {@link #getClasses() classes} array is not empty
	 * @see #hasResources()
	 * @see #hasLocations()
	 */
	public boolean hasClasses() {
		return !ObjectUtils.isEmpty(getClasses());
	}

	/**
	 * Determine if this {@code ContextConfigurationAttributes} instance has
	 * either path-based resource locations or class-based resources.
	 *
	 * @return {@code true} if either the {@link #getLocations() locations}
	 * or the {@link #getClasses() classes} array is not empty
	 * @see #hasLocations()
	 * @see #hasClasses()
	 */
	public boolean hasResources() {
		return hasLocations() || hasClasses();
	}

	/**
	 * Get the {@code inheritLocations} flag that was declared via
	 * {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * @return the {@code inheritLocations} flag
	 * @see ContextConfiguration#inheritLocations
	 */
	public boolean isInheritLocations() {
		return inheritLocations;
	}

	/**
	 * Get the {@code ApplicationContextInitializer} classes that were declared via
	 * {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * @return the {@code ApplicationContextInitializer} classes
	 * @since 3.2
	 */
	public Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] getInitializers() {
		return initializers;
	}

	/**
	 * Get the {@code inheritInitializers} flag that was declared via
	 * {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * @return the {@code inheritInitializers} flag
	 * @since 3.2
	 */
	public boolean isInheritInitializers() {
		return inheritInitializers;
	}

	/**
	 * Get the {@code ContextLoader} class that was declared via
	 * {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * @return the {@code ContextLoader} class
	 * @see ContextConfiguration#loader
	 */
	public Class<? extends ContextLoader> getContextLoaderClass() {
		return contextLoaderClass;
	}

	/**
	 * Get the name of the context hierarchy level that was declared via
	 * {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * @return the name of the context hierarchy level or {@code null} if not applicable
	 * @see ContextConfiguration#name()
	 * @since 3.2.2
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Generate a unique hash code for all properties of this
	 * {@code ContextConfigurationAttributes} instance excluding the
	 * {@linkplain #getName() name}.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + declaringClass.hashCode();
		result = prime * result + Arrays.hashCode(locations);
		result = prime * result + Arrays.hashCode(classes);
		result = prime * result + (inheritLocations ? 1231 : 1237);
		result = prime * result + Arrays.hashCode(initializers);
		result = prime * result + (inheritInitializers ? 1231 : 1237);
		result = prime * result + contextLoaderClass.hashCode();
		return result;
	}

	/**
	 * Determine if the supplied object is equal to this
	 * {@code ContextConfigurationAttributes} instance by comparing both object's
	 * {@linkplain #getDeclaringClass() declaring class},
	 * {@linkplain #getLocations() locations},
	 * {@linkplain #getClasses() annotated classes},
	 * {@linkplain #isInheritLocations() inheritLocations flag},
	 * {@linkplain #getInitializers() context initializer classes},
	 * {@linkplain #isInheritInitializers() inheritInitializers flag}, and the
	 * {@link #getContextLoaderClass() ContextLoader class}.
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ContextConfigurationAttributes)) {
			return false;
		}

		final ContextConfigurationAttributes that = (ContextConfigurationAttributes) obj;

		if (this.declaringClass == null) {
			if (that.declaringClass != null) {
				return false;
			}
		}
		else if (!this.declaringClass.equals(that.declaringClass)) {
			return false;
		}

		if (!Arrays.equals(this.locations, that.locations)) {
			return false;
		}

		if (!Arrays.equals(this.classes, that.classes)) {
			return false;
		}

		if (this.inheritLocations != that.inheritLocations) {
			return false;
		}

		if (!Arrays.equals(this.initializers, that.initializers)) {
			return false;
		}

		if (this.inheritInitializers != that.inheritInitializers) {
			return false;
		}

		if (this.contextLoaderClass == null) {
			if (that.contextLoaderClass != null) {
				return false;
			}
		}
		else if (!this.contextLoaderClass.equals(that.contextLoaderClass)) {
			return false;
		}

		return true;
	}

	/**
	 * Provide a String representation of the context configuration attributes
	 * and declaring class.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)//
		.append("declaringClass", declaringClass.getName())//
		.append("locations", ObjectUtils.nullSafeToString(locations))//
		.append("classes", ObjectUtils.nullSafeToString(classes))//
		.append("inheritLocations", inheritLocations)//
		.append("initializers", ObjectUtils.nullSafeToString(initializers))//
		.append("inheritInitializers", inheritInitializers)//
		.append("name", name)//
		.append("contextLoaderClass", contextLoaderClass.getName())//
		.toString();
	}

}
