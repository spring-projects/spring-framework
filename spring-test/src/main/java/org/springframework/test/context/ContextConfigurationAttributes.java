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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

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

	private Map<Class<?>, String[]> extendedLocations = new HashMap<Class<?>, String[]>();

	private String[] locations;

	private Class<?>[] classes;

	private boolean inheritLocations;

	private Class<? extends ContextLoader> contextLoaderClass;

	private Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] initializers;

	private boolean inheritInitializers;


	/**
	 * Resolve resource locations from the {@link ContextConfiguration#locations() locations}
	 * and {@link ContextConfiguration#value() value} attributes of the supplied
	 * {@link ContextConfiguration} annotation.
	 *
	 * @throws IllegalStateException if both the locations and value attributes have been declared
	 */
	private static String[] resolveLocations(Class<?> declaringClass, ContextConfiguration contextConfiguration) {
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
		} else if (!ObjectUtils.isEmpty(valueLocations)) {
			locations = valueLocations;
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
			contextConfiguration.inheritInitializers(), contextConfiguration.loader());
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
	 * {@link #ContextConfigurationAttributes(Class, String[], Class[], boolean, Class[], boolean, Class)}
	 * instead
	 */
	@Deprecated
	public ContextConfigurationAttributes(Class<?> declaringClass, String[] locations, Class<?>[] classes,
			boolean inheritLocations, Class<? extends ContextLoader> contextLoaderClass) {
		this(declaringClass, locations, classes, inheritLocations, null, true, contextLoaderClass);
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
		this.contextLoaderClass = contextLoaderClass;
	}

	/**
	 * This method adds a meta-annotation in the building attributes
	 * @param annotationClass the Annotation that contains the {@link ContextConfiguration}
	 * @param contextConfiguration the annotation from which to retrieve the attributes
	 */
	public void addConfigurationFromMetaAnnotations(Class<?> annotationClass, ContextConfiguration contextConfiguration) {

		// we cannot merge all the locations[] right here: we need to store the 
		// extended locations for future use, because the AbstractContextLoader
		// (in the processContextConfiguration method)
		// needs all the @Annotation.class = locations[] couples to generate
		// the default values and harmonize the locations[] strings
		String[] locations = resolveLocations(annotationClass, contextConfiguration);
		extendedLocations.put(annotationClass, locations);

		// update classes-configuration-list and initializers-list, merging the arrays
		// TODO: is OK to not-checking possible duplications?
		Class<?>[] classes =contextConfiguration.classes();
		this.classes = ObjectUtils.mergeArrays(this.classes, classes);

		Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] initializers = contextConfiguration.initializers();
		this.initializers = ObjectUtils.mergeArrays(this.initializers, initializers);

		// bitwise-and for inheritance settings
		// TODO is OK this behavior? or is preferred to exclude the meta-annotations in the
		// settings of inheritance of locations and initializers?
		inheritLocations &= contextConfiguration.inheritLocations();
		inheritInitializers &= contextConfiguration.inheritInitializers();

		// override contextLoaderClass only if null or default
		if (contextLoaderClass == null && contextLoaderClass.equals(ContextLoader.class)) {
			contextLoaderClass = contextConfiguration.loader();
		}
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
	 * path-based resource locations (or its meta-annotations).
	 *
	 * @return {@code true} if the {@link #getLocations() locations} array is not empty
	 * @see #hasResources()
	 * @see #hasClasses()
	 * @see #hasExtendedLocations()
	 */
	public boolean hasLocations() {
		return !ObjectUtils.isEmpty(getLocations()) || hasExtendedLocations();
	}

	/**
	 * Determine if the {@code ContextConfigurationAttributes} in meta-annotations has
	 * path-based resource locations.
	 * 
	 * @return {@code true} if one of the extendedLocations contains a not-empty array
	 */
	private boolean hasExtendedLocations() {
		if (!extendedLocations.isEmpty()) {
			for (String[] value : extendedLocations.values()) {
				if (!ObjectUtils.isEmpty(value)) {
					return true;
				}
			}
		}
		return false;
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
	 * Get the {@code Map} with the form {@code @Annotation} = {@code locations[]}
	 * declared in the meta-annotations of the declaring test class
	 * 
	 * @return the {@code Map} with the meta-annotation = locations[]
	 */
	public Map<Class<?>, String[]> getExtendedLocations() {
		return extendedLocations;
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
		.append("contextLoaderClass", contextLoaderClass.getName())//
		.append("metaAnnotations", extendedLocations.keySet().toString())//
		.toString();
	}

}
