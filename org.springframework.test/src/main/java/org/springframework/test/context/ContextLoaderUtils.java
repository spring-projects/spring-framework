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
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ResourceTypeAwareContextLoader.ResourceType;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods for working with {@link ContextLoader ContextLoaders}
 * and resource locations.
 * 
 * @author Sam Brannen
 * @since 3.1
 * @see ContextLoader
 */
abstract class ContextLoaderUtils {

	// TODO Consider refactoring ContextLoaderUtils into a stateful
	// ContextLoaderResolver.

	private static final Log logger = LogFactory.getLog(ContextLoaderUtils.class);

	private static final String STANDARD_DEFAULT_CONTEXT_LOADER_CLASS_NAME = "org.springframework.test.context.support.GenericXmlContextLoader";

	private static final ResourcePathLocationsResolver resourcePathLocationsResolver = new ResourcePathLocationsResolver();
	private static final ClassNameLocationsResolver classNameLocationsResolver = new ClassNameLocationsResolver();


	/**
	 * Resolves the {@link ContextLoader} {@link Class} to use for the
	 * supplied {@link Class testClass} and then instantiates and returns
	 * that <code>ContextLoader</code>.
	 * 
	 * <p>If the supplied <code>defaultContextLoaderClassName</code> is
	 * <code>null</code> or <em>empty</em>, the <em>standard</em>
	 * default context loader class name ({@value #STANDARD_DEFAULT_CONTEXT_LOADER_CLASS_NAME})
	 * will be used. For details on the class resolution process, see
	 * {@link #resolveContextLoaderClass(Class, String)}.
	 * @param testClass the test class for which the <code>ContextLoader</code>
	 * should be resolved (must not be <code>null</code>)
	 * @param defaultContextLoaderClassName the name of the default
	 * <code>ContextLoader</code> class to use (may be <code>null</code>)
	 * @return the resolved <code>ContextLoader</code> for the supplied
	 * <code>testClass</code> (never <code>null</code>)
	 * @see #resolveContextLoaderClass(Class, String)
	 */
	static ContextLoader resolveContextLoader(Class<?> testClass, String defaultContextLoaderClassName) {
		Assert.notNull(testClass, "Test class must not be null");

		if (!StringUtils.hasText(defaultContextLoaderClassName)) {
			defaultContextLoaderClassName = STANDARD_DEFAULT_CONTEXT_LOADER_CLASS_NAME;
		}

		Class<? extends ContextLoader> contextLoaderClass = resolveContextLoaderClass(testClass,
			defaultContextLoaderClassName);

		return (ContextLoader) BeanUtils.instantiateClass(contextLoaderClass);
	}

	/**
	 * Resolves the {@link ContextLoader} {@link Class} to use for the supplied
	 * {@link Class test class}.
	 * 
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
	 * 
	 * @param clazz the class for which to resolve the <code>ContextLoader</code>
	 * class; must not be <code>null</code>
	 * @param defaultContextLoaderClassName the name of the default
	 * <code>ContextLoader</code> class to use; must not be <code>null</code> or empty
	 * @return the <code>ContextLoader</code> class to use for the specified class
	 * (never <code>null</code>)
	 * @throws IllegalArgumentException if {@link ContextConfiguration
	 * &#064;ContextConfiguration} is not <em>present</em> on the supplied class
	 */
	@SuppressWarnings("unchecked")
	static Class<? extends ContextLoader> resolveContextLoaderClass(Class<?> clazz, String defaultContextLoaderClassName) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.hasText(defaultContextLoaderClassName, "Default ContextLoader class name must not be null or empty");

		Class<ContextConfiguration> annotationType = ContextConfiguration.class;
		Class<?> declaringClass = AnnotationUtils.findAnnotationDeclaringClass(annotationType, clazz);
		Assert.notNull(declaringClass, String.format(
			"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]", annotationType,
			clazz));

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
			if (logger.isTraceEnabled()) {
				ContextConfiguration contextConfiguration = clazz.getAnnotation(annotationType);
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
	 * Resolves {@link ApplicationContext} resource locations for the supplied
	 * {@link Class class}, using the supplied {@link ContextLoader} to
	 * {@link ContextLoader#processLocations(Class, String...) process} the
	 * locations.
	 * 
	 * <p>Note that the {@link ContextConfiguration#inheritLocations()
	 * inheritLocations} flag of {@link ContextConfiguration
	 * &#064;ContextConfiguration} will be taken into consideration.
	 * Specifically, if the <code>inheritLocations</code> flag is set to
	 * <code>true</code>, locations defined in the annotated class will be
	 * appended to the locations defined in superclasses.
	 * 
	 * @param contextLoader the ContextLoader to use for processing the
	 * locations (must not be <code>null</code>)
	 * @param clazz the class for which to resolve the resource locations (must
	 * not be <code>null</code>)
	 * @return the list of ApplicationContext resource locations for the
	 * specified class, including locations from superclasses if appropriate
	 * (never <code>null</code>)
	 * @throws IllegalArgumentException if {@link ContextConfiguration
	 * &#064;ContextConfiguration} is not <em>present</em> on the supplied class
	 */
	static String[] resolveContextLocations(ContextLoader contextLoader, Class<?> clazz) {
		Assert.notNull(contextLoader, "ContextLoader must not be null");
		Assert.notNull(clazz, "Class must not be null");

		boolean processConfigurationClasses = (contextLoader instanceof ResourceTypeAwareContextLoader)
				&& ResourceType.CLASSES == ((ResourceTypeAwareContextLoader) contextLoader).getResourceType();
		LocationsResolver locationsResolver = processConfigurationClasses ? classNameLocationsResolver
				: resourcePathLocationsResolver;

		Class<ContextConfiguration> annotationType = ContextConfiguration.class;
		Class<?> declaringClass = AnnotationUtils.findAnnotationDeclaringClass(annotationType, clazz);
		Assert.notNull(declaringClass, String.format(
			"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]", annotationType,
			clazz));

		final List<String> locationsList = new ArrayList<String>();

		while (declaringClass != null) {
			ContextConfiguration contextConfiguration = declaringClass.getAnnotation(annotationType);

			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @ContextConfiguration [%s] for declaring class [%s].",
					contextConfiguration, declaringClass));
			}

			String[] resolvedLocations = locationsResolver.resolveLocations(contextConfiguration, declaringClass);
			String[] processedLocations = contextLoader.processLocations(declaringClass, resolvedLocations);
			locationsList.addAll(0, Arrays.asList(processedLocations));

			declaringClass = contextConfiguration.inheritLocations() ? AnnotationUtils.findAnnotationDeclaringClass(
				annotationType, declaringClass.getSuperclass()) : null;
		}

		return StringUtils.toStringArray(locationsList);
	}

	/**
	 * Resolves <em>active bean definition profiles</em> for the supplied
	 * {@link Class class}.
	 * 
	 * <p>Note that the {@link ActiveProfiles#inheritProfiles() inheritProfiles}
	 * flag of {@link ActiveProfiles &#064;ActiveProfiles} will be taken into
	 * consideration. Specifically, if the <code>inheritProfiles</code> flag is
	 * set to <code>true</code>, profiles defined in the annotated class will be
	 * merged with those defined in superclasses.
	 *
	 * @param clazz the class for which to resolve the active profiles (must
	 * not be <code>null</code>)
	 * @return the set of active profiles for the specified class, including
	 * active profiles from superclasses if appropriate (never <code>null</code>)
	 */
	static String[] resolveActiveProfiles(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");

		Class<ActiveProfiles> annotationType = ActiveProfiles.class;
		Class<?> declaringClass = AnnotationUtils.findAnnotationDeclaringClass(annotationType, clazz);

		if (declaringClass == null && logger.isDebugEnabled()) {
			logger.debug(String.format(
				"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
				annotationType, clazz));
		}

		// Active profiles must be sorted due to cache key generation in
		// TestContext. Specifically, profile sets {foo,bar} and {bar,foo}
		// must both result in the same array (e.g., [bar,foo]).
		final SortedSet<String> activeProfiles = new TreeSet<String>();

		while (declaringClass != null) {
			ActiveProfiles annotation = declaringClass.getAnnotation(annotationType);

			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @ActiveProfiles [%s] for declaring class [%s].", annotation,
					declaringClass));
			}

			String[] profiles = annotation.profiles();
			String[] valueProfiles = annotation.value();

			if (!ObjectUtils.isEmpty(valueProfiles) && !ObjectUtils.isEmpty(profiles)) {
				String msg = String.format("Test class [%s] has been configured with @ActiveProfiles' 'value' [%s] "
						+ "and 'profiles' [%s] attributes. Only one declaration of active bean "
						+ "definition profiles is permitted per @ActiveProfiles annotation.", declaringClass,
					ObjectUtils.nullSafeToString(valueProfiles), ObjectUtils.nullSafeToString(profiles));
				logger.error(msg);
				throw new IllegalStateException(msg);
			}
			else if (!ObjectUtils.isEmpty(valueProfiles)) {
				profiles = valueProfiles;
			}

			for (String profile : profiles) {
				if (StringUtils.hasText(profile)) {
					activeProfiles.add(profile.trim());
				}
			}

			declaringClass = annotation.inheritProfiles() ? AnnotationUtils.findAnnotationDeclaringClass(
				annotationType, declaringClass.getSuperclass()) : null;
		}

		return StringUtils.toStringArray(activeProfiles);
	}

	/**
	 * TODO Document resolveContextConfigurationAttributes().
	 *
	 * @param clazz
	 * @return
	 */
	static List<ContextConfigurationAttributes> resolveContextConfigurationAttributes(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");

		final List<ContextConfigurationAttributes> attributesList = new ArrayList<ContextConfigurationAttributes>();

		Class<ContextConfiguration> annotationType = ContextConfiguration.class;
		Class<?> declaringClass = AnnotationUtils.findAnnotationDeclaringClass(annotationType, clazz);
		Assert.notNull(declaringClass, String.format(
			"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]", annotationType,
			clazz));

		while (declaringClass != null) {
			ContextConfiguration contextConfiguration = declaringClass.getAnnotation(annotationType);

			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @ContextConfiguration [%s] for declaring class [%s].",
					contextConfiguration, declaringClass));
			}

			ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(declaringClass,
				contextConfiguration);
			if (logger.isTraceEnabled()) {
				logger.trace("Resolved context configuration attributes: " + attributes);
			}

			attributesList.add(0, attributes);

			declaringClass = contextConfiguration.inheritLocations() ? AnnotationUtils.findAnnotationDeclaringClass(
				annotationType, declaringClass.getSuperclass()) : null;
		}

		return attributesList;
	}

	/**
	 * TODO Document buildMergedContextConfiguration().
	 *
	 * @param testClass
	 * @param defaultContextLoaderClassName
	 * @return
	 */
	static MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass,
			String defaultContextLoaderClassName) {

		ContextLoader contextLoader = resolveContextLoader(testClass, defaultContextLoaderClassName);

		// TODO Merge locations from List<ContextConfigurationAttributes>
		String[] locations = resolveContextLocations(contextLoader, testClass);

		// TODO Merge classes from List<ContextConfigurationAttributes>
		Class<?>[] classes = {};

		String[] activeProfiles = resolveActiveProfiles(testClass);

		return new MergedContextConfiguration(testClass, locations, classes, activeProfiles, contextLoader);
	}


	/**
	 * Strategy interface for resolving application context resource locations.
	 * 
	 * <p>The semantics of the resolved locations are implementation-dependent.
	 */
	private static interface LocationsResolver {

		/**
		 * Resolves application context resource locations for the supplied
		 * {@link ContextConfiguration} annotation and the class which declared it.
		 * @param contextConfiguration the <code>ContextConfiguration</code>
		 * for which to resolve resource locations
		 * @param declaringClass the class that declared <code>ContextConfiguration</code>
		 * @return an array of application context resource locations
		 * (can be <code>null</code> or empty)
		 */
		String[] resolveLocations(ContextConfiguration contextConfiguration, Class<?> declaringClass);
	}

	/**
	 * <code>LocationsResolver</code> that resolves locations as Strings,
	 * which are assumed to be path-based resources.
	 */
	private static final class ResourcePathLocationsResolver implements LocationsResolver {

		/**
		 * Resolves path-based resources from the {@link ContextConfiguration#locations() locations}
		 * and {@link ContextConfiguration#value() value} attributes of the supplied
		 * {@link ContextConfiguration} annotation.
		 * 
		 * <p>Ignores the {@link ContextConfiguration#classes() classes} attribute. 
		 * @throws IllegalStateException if both the locations and value
		 * attributes have been declared
		 */
		public String[] resolveLocations(ContextConfiguration contextConfiguration, Class<?> declaringClass) {
			return ContextConfigurationAttributes.resolveLocations(declaringClass, contextConfiguration);
		}
	}

	/**
	 * <code>LocationsResolver</code> that converts classes to fully qualified class names.
	 */
	private static final class ClassNameLocationsResolver implements LocationsResolver {

		/**
		 * Resolves class names from the {@link ContextConfiguration#classes() classes}
		 * attribute of the supplied {@link ContextConfiguration} annotation.
		 * 
		 * <p>Ignores the {@link ContextConfiguration#locations() locations}
		 * and {@link ContextConfiguration#value() value} attributes.
		 */
		public String[] resolveLocations(ContextConfiguration contextConfiguration, Class<?> declaringClass) {

			String[] classNames = null;

			Class<?>[] configClasses = contextConfiguration.classes();
			if (!ObjectUtils.isEmpty(configClasses)) {
				classNames = new String[configClasses.length];

				for (int i = 0; i < configClasses.length; i++) {
					classNames[i] = configClasses[i].getName();
				}
			}

			return classNames;
		}
	}

}
