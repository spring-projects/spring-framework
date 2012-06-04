/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.springframework.beans.BeanUtils.instantiateClass;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotationDeclaringClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods for working with {@link ContextLoader ContextLoaders} and
 * {@link SmartContextLoader SmartContextLoaders} and resolving resource locations,
 * annotated classes, and active bean definition profiles.
 * 
 * @author Sam Brannen
 * @since 3.1
 * @see ContextLoader
 * @see SmartContextLoader
 * @see ContextConfiguration
 * @see ContextConfigurationAttributes
 * @see ActiveProfiles
 * @see MergedContextConfiguration
 */
abstract class ContextLoaderUtils {

	private static final Log logger = LogFactory.getLog(ContextLoaderUtils.class);

	private static final String DEFAULT_CONTEXT_LOADER_CLASS_NAME = "org.springframework.test.context.support.DelegatingSmartContextLoader";


	private ContextLoaderUtils() {
		/* no-op */
	}

	/**
	 * Resolve the {@link ContextLoader} {@link Class class} to use for the
	 * supplied {@link Class testClass} and then instantiate and return that
	 * {@code ContextLoader}.
	 *
	 * <p>If the supplied <code>defaultContextLoaderClassName</code> is
	 * <code>null</code> or <em>empty</em>, the <em>standard</em>
	 * default context loader class name {@value #DEFAULT_CONTEXT_LOADER_CLASS_NAME}
	 * will be used. For details on the class resolution process, see
	 * {@link #resolveContextLoaderClass()}.
	 *
	 * @param testClass the test class for which the {@code ContextLoader}
	 * should be resolved (must not be <code>null</code>)
	 * @param defaultContextLoaderClassName the name of the default
	 * {@code ContextLoader} class to use (may be <code>null</code>)
	 * @return the resolved {@code ContextLoader} for the supplied
	 * <code>testClass</code> (never <code>null</code>)
	 * @see #resolveContextLoaderClass()
	 */
	static ContextLoader resolveContextLoader(Class<?> testClass, String defaultContextLoaderClassName) {
		Assert.notNull(testClass, "Test class must not be null");

		if (!StringUtils.hasText(defaultContextLoaderClassName)) {
			defaultContextLoaderClassName = DEFAULT_CONTEXT_LOADER_CLASS_NAME;
		}

		Class<? extends ContextLoader> contextLoaderClass = resolveContextLoaderClass(testClass,
			defaultContextLoaderClassName);

		return instantiateClass(contextLoaderClass, ContextLoader.class);
	}

	/**
	 * Resolve the {@link ContextLoader} {@link Class} to use for the supplied
	 * {@link Class testClass}.
	 *
	 * <ol>
	 * <li>If the {@link ContextConfiguration#loader() loader} attribute of
	 * {@link ContextConfiguration &#064;ContextConfiguration} is configured
	 * with an explicit class, that class will be returned.</li>
	 * <li>If a <code>loader</code> class is not specified, the class hierarchy
	 * will be traversed to find a parent class annotated with
	 * {@code @ContextConfiguration}; go to step #1.</li>
	 * <li>If no explicit <code>loader</code> class is found after traversing
	 * the class hierarchy, an attempt will be made to load and return the class
	 * with the supplied <code>defaultContextLoaderClassName</code>.</li>
	 * </ol>
	 *
	 * @param testClass the class for which to resolve the {@code ContextLoader}
	 * class; must not be <code>null</code>
	 * @param defaultContextLoaderClassName the name of the default
	 * {@code ContextLoader} class to use; must not be <code>null</code> or empty
	 * @return the {@code ContextLoader} class to use for the supplied test class
	 * @throws IllegalArgumentException if {@code @ContextConfiguration} is not
	 * <em>present</em> on the supplied test class 
	 * @throws IllegalStateException if the default {@code ContextLoader} class
	 * could not be loaded 
	 */
	@SuppressWarnings("unchecked")
	static Class<? extends ContextLoader> resolveContextLoaderClass(Class<?> testClass,
			String defaultContextLoaderClassName) {
		Assert.notNull(testClass, "Class must not be null");
		Assert.hasText(defaultContextLoaderClassName, "Default ContextLoader class name must not be null or empty");

		Class<ContextConfiguration> annotationType = ContextConfiguration.class;
		Class<?> declaringClass = findAnnotationDeclaringClass(annotationType, testClass);
		Assert.notNull(declaringClass, String.format(
			"Could not find an 'annotation declaring class' for annotation type [%s] and test class [%s]",
			annotationType, testClass));

		while (declaringClass != null) {
			ContextConfiguration contextConfiguration = declaringClass.getAnnotation(annotationType);

			if (logger.isTraceEnabled()) {
				logger.trace(String.format(
					"Processing ContextLoader for @ContextConfiguration [%s] and declaring class [%s]",
					contextConfiguration, declaringClass));
			}

			Class<? extends ContextLoader> contextLoaderClass = contextConfiguration.loader();
			if (!ContextLoader.class.equals(contextLoaderClass)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
						"Found explicit ContextLoader class [%s] for @ContextConfiguration [%s] and declaring class [%s]",
						contextLoaderClass, contextConfiguration, declaringClass));
				}
				return contextLoaderClass;
			}

			declaringClass = findAnnotationDeclaringClass(annotationType, declaringClass.getSuperclass());
		}

		try {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Using default ContextLoader class [%s] for test class [%s]",
					defaultContextLoaderClassName, testClass));
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
	 * Resolve the list of {@link ContextConfigurationAttributes configuration
	 * attributes} for the supplied {@link Class class} and its superclasses.
	 *
	 * <p>Note that the {@link ContextConfiguration#inheritLocations
	 * inheritLocations} flag of {@link ContextConfiguration
	 * &#064;ContextConfiguration} will be taken into consideration.
	 * Specifically, if the <code>inheritLocations</code> flag is set to
	 * <code>true</code>, configuration attributes defined in the test
	 * class will be appended to the configuration attributes defined in
	 * superclasses.
	 *
	 * @param clazz the class for which to resolve the configuration attributes (must
	 * not be <code>null</code>)
	 * @return the list of configuration attributes for the specified class,
	 * including configuration attributes from superclasses if appropriate
	 * (never <code>null</code>)
	 * @throws IllegalArgumentException if the supplied class is <code>null</code> or
	 * if {@code @ContextConfiguration} is not <em>present</em> on the supplied class
	 */
	static List<ContextConfigurationAttributes> resolveContextConfigurationAttributes(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");

		final List<ContextConfigurationAttributes> attributesList = new ArrayList<ContextConfigurationAttributes>();

		Class<ContextConfiguration> annotationType = ContextConfiguration.class;
		Class<?> declaringClass = findAnnotationDeclaringClass(annotationType, clazz);
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

			declaringClass = contextConfiguration.inheritLocations() ? findAnnotationDeclaringClass(annotationType,
				declaringClass.getSuperclass()) : null;
		}

		return attributesList;
	}

	/**
	 * Resolve <em>active bean definition profiles</em> for the supplied {@link Class}.
	 *
	 * <p>Note that the {@link ActiveProfiles#inheritProfiles inheritProfiles}
	 * flag of {@link ActiveProfiles &#064;ActiveProfiles} will be taken into
	 * consideration. Specifically, if the <code>inheritProfiles</code> flag is
	 * set to <code>true</code>, profiles defined in the test class will be
	 * merged with those defined in superclasses.
	 *
	 * @param clazz the class for which to resolve the active profiles (must
	 * not be <code>null</code>)
	 * @return the set of active profiles for the specified class, including
	 * active profiles from superclasses if appropriate (never <code>null</code>)
	 * @see org.springframework.test.context.ActiveProfiles
	 * @see org.springframework.context.annotation.Profile
	 */
	static String[] resolveActiveProfiles(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");

		Class<ActiveProfiles> annotationType = ActiveProfiles.class;
		Class<?> declaringClass = findAnnotationDeclaringClass(annotationType, clazz);

		if (declaringClass == null && logger.isDebugEnabled()) {
			logger.debug(String.format(
				"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
				annotationType, clazz));
		}

		final Set<String> activeProfiles = new HashSet<String>();

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

			declaringClass = annotation.inheritProfiles() ? findAnnotationDeclaringClass(annotationType,
				declaringClass.getSuperclass()) : null;
		}

		return StringUtils.toStringArray(activeProfiles);
	}

	/**
	 * Build the {@link MergedContextConfiguration merged context configuration}
	 * for the supplied {@link Class testClass} and
	 * <code>defaultContextLoaderClassName</code>.
	 *
	 * @param testClass the test class for which the {@code MergedContextConfiguration}
	 * should be built (must not be <code>null</code>)
	 * @param defaultContextLoaderClassName the name of the default
	 * {@code ContextLoader} class to use (may be <code>null</code>)
	 * @return the merged context configuration
	 * @see #resolveContextLoader()
	 * @see #resolveContextConfigurationAttributes()
	 * @see SmartContextLoader#processContextConfiguration()
	 * @see ContextLoader#processLocations()
	 * @see #resolveActiveProfiles()
	 * @see MergedContextConfiguration
	 */
	static MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass,
			String defaultContextLoaderClassName) {

		final ContextLoader contextLoader = resolveContextLoader(testClass, defaultContextLoaderClassName);
		final List<ContextConfigurationAttributes> configAttributesList = resolveContextConfigurationAttributes(testClass);
		final List<String> locationsList = new ArrayList<String>();
		final List<Class<?>> classesList = new ArrayList<Class<?>>();

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format(
					"Processing locations and classes for context configuration attributes [%s]", configAttributes));
			}

			if (contextLoader instanceof SmartContextLoader) {
				SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
				smartContextLoader.processContextConfiguration(configAttributes);
				locationsList.addAll(Arrays.asList(configAttributes.getLocations()));
				classesList.addAll(Arrays.asList(configAttributes.getClasses()));
			}
			else {
				String[] processedLocations = contextLoader.processLocations(configAttributes.getDeclaringClass(),
					configAttributes.getLocations());
				locationsList.addAll(Arrays.asList(processedLocations));
				// Legacy ContextLoaders don't know how to process classes
			}
		}

		String[] locations = StringUtils.toStringArray(locationsList);
		Class<?>[] classes = ClassUtils.toClassArray(classesList);
		String[] activeProfiles = resolveActiveProfiles(testClass);

		return new MergedContextConfiguration(testClass, locations, classes, activeProfiles, contextLoader);
	}

}
