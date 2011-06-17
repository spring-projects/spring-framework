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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods for working with {@link ContextLoader ContextLoaders},
 * resource locations and classes, and active bean definition profiles.
 * 
 * @author Sam Brannen
 * @since 3.1
 * @see ContextLoader
 * @see ContextConfiguration
 * @see ContextConfigurationAttributes
 * @see ActiveProfiles
 * @see MergedContextConfiguration
 */
abstract class ContextLoaderUtils {

	private static final Log logger = LogFactory.getLog(ContextLoaderUtils.class);

	private static final String STANDARD_DEFAULT_CONTEXT_LOADER_CLASS_NAME = "org.springframework.test.context.support.GenericXmlContextLoader";


	/**
	 * TODO Document resolveContextConfigurationAttributes().
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
	 * Resolves the {@link ContextLoader} {@link Class} to use for the
	 * supplied {@link Class testClass} and then instantiates and returns
	 * that <code>ContextLoader</code>.
	 * 
	 * <p>If the supplied <code>defaultContextLoaderClassName</code> is
	 * <code>null</code> or <em>empty</em>, the <em>standard</em>
	 * default context loader class name ({@value #STANDARD_DEFAULT_CONTEXT_LOADER_CLASS_NAME})
	 * will be used. For details on the class resolution process, see
	 * {@link #resolveContextLoaderClass(Class, String)}.
	 * 
	 * @param testClass the test class for which the <code>ContextLoader</code>
	 * should be resolved (must not be <code>null</code>)
	 * @param configAttributesList TODO Document parameter
	 * @param defaultContextLoaderClassName the name of the default
	 * <code>ContextLoader</code> class to use (may be <code>null</code>)
	 * 
	 * @return the resolved <code>ContextLoader</code> for the supplied
	 * <code>testClass</code> (never <code>null</code>)
	 * @see #resolveContextLoaderClass(Class, String)
	 */
	static ContextLoader resolveContextLoader(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList, String defaultContextLoaderClassName) {
		Assert.notNull(testClass, "Test class must not be null");
		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be null or empty");

		if (!StringUtils.hasText(defaultContextLoaderClassName)) {
			defaultContextLoaderClassName = STANDARD_DEFAULT_CONTEXT_LOADER_CLASS_NAME;
		}

		Class<? extends ContextLoader> contextLoaderClass = resolveContextLoaderClass(testClass, configAttributesList,
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
	 * @param testClass the class for which to resolve the <code>ContextLoader</code>
	 * class; must not be <code>null</code>
	 * @param configAttributesList TODO Document parameter
	 * @param defaultContextLoaderClassName the name of the default
	 * <code>ContextLoader</code> class to use; must not be <code>null</code> or empty
	 * 
	 * @return the <code>ContextLoader</code> class to use for the specified class
	 * (never <code>null</code>)
	 * @throws IllegalArgumentException if {@link ContextConfiguration
	 * &#064;ContextConfiguration} is not <em>present</em> on the supplied class
	 */
	@SuppressWarnings("unchecked")
	static Class<? extends ContextLoader> resolveContextLoaderClass(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList, String defaultContextLoaderClassName) {
		Assert.notNull(testClass, "Class must not be null");
		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be null or empty");
		Assert.hasText(defaultContextLoaderClassName, "Default ContextLoader class name must not be null or empty");

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format(
					"Processing ContextLoader for context configuration attributes [%s] and test class [%s]",
					configAttributes, testClass));
			}

			Class<? extends ContextLoader> contextLoaderClass = configAttributes.getContextLoaderClass();
			if (!ContextLoader.class.equals(contextLoaderClass)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
						"Found explicit ContextLoader class [%s] for context configuration attributes [%s] and test class [%s]",
						contextLoaderClass, configAttributes, testClass));
				}
				return contextLoaderClass;
			}
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
	 * 
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

			declaringClass = annotation.inheritProfiles() ? AnnotationUtils.findAnnotationDeclaringClass(
				annotationType, declaringClass.getSuperclass()) : null;
		}

		return StringUtils.toStringArray(activeProfiles);
	}

	/*
	 * Resolves {@link ApplicationContext} resource locations for the supplied
	 * {@link Class class}, using the supplied {@link ContextLoader} to {@link
	 * ContextLoader#processLocations(Class, String...) process} the locations.
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
	 * 
	 * @param clazz the class for which to resolve the resource locations (must
	 * not be <code>null</code>)
	 * 
	 * @return the list of ApplicationContext resource locations for the
	 * specified class, including locations from superclasses if appropriate
	 * (never <code>null</code>)
	 * 
	 * @throws IllegalArgumentException if {@link ContextConfiguration
	 * &#064;ContextConfiguration} is not <em>present</em> on the supplied class
	 */

	/**
	 * TODO Document buildMergedContextConfiguration().
	 */
	static MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass,
			String defaultContextLoaderClassName) {

		List<ContextConfigurationAttributes> configAttributesList = resolveContextConfigurationAttributes(testClass);

		ContextLoader contextLoader = resolveContextLoader(testClass, configAttributesList,
			defaultContextLoaderClassName);

		// Algorithm:
		// - iterate over config attributes
		// -- let loader process locations
		// -- let loader process classes, if it's a SmartContextLoader

		final List<String> locationsList = new ArrayList<String>();
		final List<Class<?>> classesList = new ArrayList<Class<?>>();

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format(
					"Processing locations and classes for context configuration attributes [%s]", configAttributes));
			}

			if (contextLoader instanceof SmartContextLoader) {
				SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
				// TODO Decide on mutability of locations and classes properties
				smartContextLoader.processContextConfigurationAttributes(configAttributes);
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
