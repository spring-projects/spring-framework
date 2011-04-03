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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods for working with {@link ContextLoader ContextLoaders}.
 * 
 * <p>TODO: Consider refactoring into a stateful ContextLoaderResolver.
 * 
 * @author Sam Brannen
 * @since 3.1
 * @see ContextLoader
 */
public abstract class ContextLoaderUtils {

	private static final Log logger = LogFactory.getLog(ContextLoaderUtils.class);

	private static final String STANDARD_DEFAULT_CONTEXT_LOADER_CLASS_NAME = "org.springframework.test.context.support.GenericXmlContextLoader";


	/**
	 * TODO Document resolveContextLoader().
	 *
	 * @param testClass the test class for which the <code>ContextLoader</code>
	 * should be resolved (must not be <code>null</code>)
	 * @param defaultContextLoaderClassName the name of the default
	 * <code>ContextLoader</code> class to use (may be <code>null</code>)
	 * @return the resolved <code>ContextLoader</code> for the supplied
	 * <code>testClass</code> 
	 * @see #resolveContextLoaderClass(Class, String)
	 */
	public static ContextLoader resolveContextLoader(Class<?> testClass, String defaultContextLoaderClassName) {
		Assert.notNull(testClass, "Test class must not be null");

		if (!StringUtils.hasText(defaultContextLoaderClassName)) {
			defaultContextLoaderClassName = STANDARD_DEFAULT_CONTEXT_LOADER_CLASS_NAME;
		}

		Class<? extends ContextLoader> contextLoaderClass = resolveContextLoaderClass(testClass,
			defaultContextLoaderClassName);

		return (ContextLoader) BeanUtils.instantiateClass(contextLoaderClass);
	}

	/**
	 * Retrieve the {@link ContextLoader} {@link Class} to use for the supplied
	 * {@link Class test class}.
	 * <ol>
	 * <li>If the {@link ContextConfiguration#loader() loader} attribute of
	 * {@link ContextConfiguration &#064;ContextConfiguration} is configured
	 * with an explicit class, that class will be returned.</li>
	 * <li>If a <code>loader</code> class is not specified, the class hierarchy
	 * will be traversed to find a parent class annotated with
	 * <code>&#064;ContextConfiguration</code>; go to step #1.</li>
	 * <li>If no explicit <code>loader</code> class is found after traversing
	 * the class hierarchy, an attempt will be made to load and return the class
	 * with the supplied <code>defaultContextLoaderClassName</code>.</li>
	 * </ol>
	 * @param clazz the class for which to retrieve <code>ContextLoader</code>
	 * class; must not be <code>null</code>
	 * @param defaultContextLoaderClassName the name of the default
	 * <code>ContextLoader</code> class to use; must not be <code>null</code> or empty
	 * @return the <code>ContextLoader</code> class to use for the specified class
	 * @throws IllegalArgumentException if {@link ContextConfiguration
	 * &#064;ContextConfiguration} is not <em>present</em> on the supplied class
	 */
	@SuppressWarnings("unchecked")
	static Class<? extends ContextLoader> resolveContextLoaderClass(Class<?> clazz, String defaultContextLoaderClassName) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.hasText(defaultContextLoaderClassName, "Default ContextLoader class name must not be null or empty");

		Class<ContextConfiguration> annotationType = ContextConfiguration.class;
		Class<?> declaringClass = AnnotationUtils.findAnnotationDeclaringClass(annotationType, clazz);
		Assert.notNull(declaringClass, "Could not find an 'annotation declaring class' for annotation type ["
				+ annotationType + "] and class [" + clazz + "]");

		while (declaringClass != null) {
			ContextConfiguration contextConfiguration = declaringClass.getAnnotation(annotationType);
			if (logger.isTraceEnabled()) {
				logger.trace("Processing ContextLoader for @ContextConfiguration [" + contextConfiguration
						+ "] and declaring class [" + declaringClass + "]");
			}

			Class<? extends ContextLoader> contextLoaderClass = contextConfiguration.loader();
			if (!ContextLoader.class.equals(contextLoaderClass)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Found explicit ContextLoader [" + contextLoaderClass
							+ "] for @ContextConfiguration [" + contextConfiguration + "] and declaring class ["
							+ declaringClass + "]");
				}
				return contextLoaderClass;
			}

			declaringClass = AnnotationUtils.findAnnotationDeclaringClass(annotationType,
				declaringClass.getSuperclass());
		}

		try {
			ContextConfiguration contextConfiguration = clazz.getAnnotation(ContextConfiguration.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using default ContextLoader class [" + defaultContextLoaderClassName
						+ "] for @ContextConfiguration [" + contextConfiguration + "] and class [" + clazz + "]");
			}
			return (Class<? extends ContextLoader>) ContextLoaderUtils.class.getClassLoader().loadClass(
				defaultContextLoaderClassName);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Could not load default ContextLoader class ["
					+ defaultContextLoaderClassName + "]. Specify @ContextConfiguration's 'loader' "
					+ "attribute or make the default loader class available.");
		}
	}

	/**
	 * Retrieve {@link ApplicationContext} resource locations for the supplied
	 * {@link Class class}, using the supplied {@link ContextLoader} to
	 * {@link ContextLoader#processLocations(Class, String...) process} the
	 * locations.
	 * <p>Note that the {@link ContextConfiguration#inheritLocations()
	 * inheritLocations} flag of {@link ContextConfiguration
	 * &#064;ContextConfiguration} will be taken into consideration.
	 * Specifically, if the <code>inheritLocations</code> flag is set to
	 * <code>true</code>, locations defined in the annotated class will be
	 * appended to the locations defined in superclasses.
	 * @param contextLoader the ContextLoader to use for processing the
	 * locations (must not be <code>null</code>)
	 * @param clazz the class for which to retrieve the resource locations (must
	 * not be <code>null</code>)
	 * @return the list of ApplicationContext resource locations for the
	 * specified class, including locations from superclasses if appropriate
	 * @throws IllegalArgumentException if {@link ContextConfiguration
	 * &#064;ContextConfiguration} is not <em>present</em> on the supplied class
	 */
	public static String[] resolveContextLocations(ContextLoader contextLoader, Class<?> clazz) {
		Assert.notNull(contextLoader, "ContextLoader must not be null");
		Assert.notNull(clazz, "Class must not be null");

		Class<ContextConfiguration> annotationType = ContextConfiguration.class;
		Class<?> declaringClass = AnnotationUtils.findAnnotationDeclaringClass(annotationType, clazz);
		Assert.notNull(declaringClass, "Could not find an 'annotation declaring class' for annotation type ["
				+ annotationType + "] and class [" + clazz + "]");

		boolean processConfigurationClasses = (contextLoader instanceof ResourceTypeAwareContextLoader)
				&& ((ResourceTypeAwareContextLoader) contextLoader).supportsClassResources();

		return processConfigurationClasses ? //
		resolveConfigurationClassNames(contextLoader, annotationType, declaringClass)
				: resolveStringLocations(contextLoader, annotationType, declaringClass);
	}

	/**
	 * TODO Document resolveStringLocations().
	 *
	 * @param contextLoader
	 * @param annotationType
	 * @param declaringClass
	 * @return
	 */
	private static String[] resolveStringLocations(ContextLoader contextLoader,
			Class<ContextConfiguration> annotationType, Class<?> declaringClass) {

		List<String> locationsList = new ArrayList<String>();

		while (declaringClass != null) {
			ContextConfiguration contextConfiguration = declaringClass.getAnnotation(annotationType);
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @ContextConfiguration [%s] for declaring class [%s].",
					contextConfiguration, declaringClass));
			}

			String[] valueLocations = contextConfiguration.value();
			String[] locations = contextConfiguration.locations();
			if (!ObjectUtils.isEmpty(valueLocations) && !ObjectUtils.isEmpty(locations)) {
				String msg = String.format(
					"Test class [%s] has been configured with @ContextConfiguration's 'value' [%s] and 'locations' [%s] attributes. Only one declaration of resource locations is permitted per @ContextConfiguration annotation.",
					declaringClass, ObjectUtils.nullSafeToString(valueLocations),
					ObjectUtils.nullSafeToString(locations));
				logger.error(msg);
				throw new IllegalStateException(msg);
			}
			else if (!ObjectUtils.isEmpty(valueLocations)) {
				locations = valueLocations;
			}

			locations = contextLoader.processLocations(declaringClass, locations);
			locationsList.addAll(0, Arrays.<String> asList(locations));
			declaringClass = contextConfiguration.inheritLocations() ? AnnotationUtils.findAnnotationDeclaringClass(
				annotationType, declaringClass.getSuperclass()) : null;
		}

		return locationsList.toArray(new String[locationsList.size()]);
	}

	/**
	 * TODO Document resolveConfigClassNames().
	 *
	 * @param contextLoader
	 * @param annotationType
	 * @param declaringClass
	 * @return
	 */
	private static String[] resolveConfigurationClassNames(ContextLoader contextLoader,
			Class<ContextConfiguration> annotationType, Class<?> declaringClass) {

		// TODO [SPR-6184] Implement recursive search for configuration classes.

		ContextConfiguration contextConfiguration = declaringClass.getAnnotation(annotationType);
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Retrieved @ContextConfiguration [%s] for declaring class [%s].",
				contextConfiguration, declaringClass));
		}

		String[] classNames = null;

		Class<?>[] configClasses = contextConfiguration.classes();
		if (!ObjectUtils.isEmpty(configClasses)) {
			classNames = new String[configClasses.length];

			for (int i = 0; i < configClasses.length; i++) {
				classNames[i] = configClasses[i].getName();
			}
		}

		return contextLoader.processLocations(declaringClass, classNames);
	}

}
