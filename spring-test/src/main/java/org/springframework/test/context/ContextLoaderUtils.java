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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.springframework.beans.BeanUtils.*;
import static org.springframework.core.annotation.AnnotationUtils.*;

/**
 * Utility methods for working with {@link ContextLoader ContextLoaders} and
 * {@link SmartContextLoader SmartContextLoaders} and resolving resource locations,
 * annotated classes, active bean definition profiles, and application context
 * initializers.
 *
 * @author Sam Brannen
 * @author Michail Nikolaev
 * @since 3.1
 * @see ContextLoader
 * @see SmartContextLoader
 * @see ContextConfiguration
 * @see ContextConfigurationAttributes
 * @see ActiveProfiles
 * @see ActiveProfilesResolver
 * @see ApplicationContextInitializer
 * @see ContextHierarchy
 * @see MergedContextConfiguration
 */
abstract class ContextLoaderUtils {

	static final String GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX = "ContextHierarchyLevel#";

	private static final Log logger = LogFactory.getLog(ContextLoaderUtils.class);

	private static final String DEFAULT_CONTEXT_LOADER_CLASS_NAME = "org.springframework.test.context.support.DelegatingSmartContextLoader";
	private static final String DEFAULT_WEB_CONTEXT_LOADER_CLASS_NAME = "org.springframework.test.context.web.WebDelegatingSmartContextLoader";

	private static final String WEB_APP_CONFIGURATION_CLASS_NAME = "org.springframework.test.context.web.WebAppConfiguration";
	private static final String WEB_MERGED_CONTEXT_CONFIGURATION_CLASS_NAME = "org.springframework.test.context.web.WebMergedContextConfiguration";


	private ContextLoaderUtils() {
		/* no-op */
	}

	/**
	 * Resolve the {@link ContextLoader} {@linkplain Class class} to use for the supplied
	 * list of {@link ContextConfigurationAttributes} and then instantiate and return that
	 * {@code ContextLoader}.
	 *
	 * <p>If the supplied {@code defaultContextLoaderClassName} is {@code null} or
	 * <em>empty</em>, depending on the absence or presence of
	 * {@link org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration} either
	 * {@code "org.springframework.test.context.support.DelegatingSmartContextLoader"} or
	 * {@code "org.springframework.test.context.web.WebDelegatingSmartContextLoader"} will
	 * be used as the default context loader class name. For details on the class
	 * resolution process, see {@link #resolveContextLoaderClass}.
	 *
	 * @param testClass the test class for which the {@code ContextLoader} should be
	 * resolved; must not be {@code null}
	 * @param configAttributesList the list of configuration attributes to process; must
	 * not be {@code null} or <em>empty</em>; must be ordered <em>bottom-up</em>
	 * (i.e., as if we were traversing up the class hierarchy)
	 * @param defaultContextLoaderClassName the name of the default {@code ContextLoader}
	 * class to use; may be {@code null} or <em>empty</em>
	 * @return the resolved {@code ContextLoader} for the supplied {@code testClass}
	 * (never {@code null})
	 * @see #resolveContextLoaderClass
	 */
	static ContextLoader resolveContextLoader(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList, String defaultContextLoaderClassName) {
		Assert.notNull(testClass, "Class must not be null");
		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be empty");

		if (!StringUtils.hasText(defaultContextLoaderClassName)) {
			Class<? extends Annotation> webAppConfigClass = loadWebAppConfigurationClass();
			defaultContextLoaderClassName = webAppConfigClass != null
					&& testClass.isAnnotationPresent(webAppConfigClass) ? DEFAULT_WEB_CONTEXT_LOADER_CLASS_NAME
					: DEFAULT_CONTEXT_LOADER_CLASS_NAME;
		}

		Class<? extends ContextLoader> contextLoaderClass = resolveContextLoaderClass(testClass, configAttributesList,
			defaultContextLoaderClassName);

		return instantiateClass(contextLoaderClass, ContextLoader.class);
	}

	/**
	 * Resolve the {@link ContextLoader} {@linkplain Class class} to use for the supplied
	 * list of {@link ContextConfigurationAttributes}.
	 *
	 * <p>Beginning with the first level in the context configuration attributes hierarchy:
	 *
	 * <ol>
	 * <li>If the {@link ContextConfigurationAttributes#getContextLoaderClass()
	 * contextLoaderClass} property of {@link ContextConfigurationAttributes} is
	 * configured with an explicit class, that class will be returned.</li>
	 * <li>If an explicit {@code ContextLoader} class is not specified at the current
	 * level in the hierarchy, traverse to the next level in the hierarchy and return to
	 * step #1.</li>
	 * <li>If no explicit {@code ContextLoader} class is found after traversing the
	 * hierarchy, an attempt will be made to load and return the class with the supplied
	 * {@code defaultContextLoaderClassName}.</li>
	 * </ol>
	 *
	 * @param testClass the class for which to resolve the {@code ContextLoader} class;
	 * must not be {@code null}; only used for logging purposes
	 * @param configAttributesList the list of configuration attributes to process; must
	 * not be {@code null} or <em>empty</em>; must be ordered <em>bottom-up</em>
	 * (i.e., as if we were traversing up the class hierarchy)
	 * @param defaultContextLoaderClassName the name of the default {@code ContextLoader}
	 * class to use; must not be {@code null} or empty
	 * @return the {@code ContextLoader} class to use for the supplied test class
	 * @throws IllegalArgumentException if {@code @ContextConfiguration} is not
	 * <em>present</em> on the supplied test class
	 * @throws IllegalStateException if the default {@code ContextLoader} class could not
	 * be loaded
	 */
	@SuppressWarnings("unchecked")
	static Class<? extends ContextLoader> resolveContextLoaderClass(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList, String defaultContextLoaderClassName) {
		Assert.notNull(testClass, "Class must not be null");
		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be empty");
		Assert.hasText(defaultContextLoaderClassName, "Default ContextLoader class name must not be null or empty");

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing ContextLoader for context configuration attributes %s",
					configAttributes));
			}

			Class<? extends ContextLoader> contextLoaderClass = configAttributes.getContextLoaderClass();
			if (!ContextLoader.class.equals(contextLoaderClass)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
						"Found explicit ContextLoader class [%s] for context configuration attributes %s",
						contextLoaderClass.getName(), configAttributes));
				}
				return contextLoaderClass;
			}
		}

		try {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Using default ContextLoader class [%s] for test class [%s]",
					defaultContextLoaderClassName, testClass.getName()));
			}
			return (Class<? extends ContextLoader>) ClassUtils.forName(defaultContextLoaderClassName,
				ContextLoaderUtils.class.getClassLoader());
		}
		catch (Throwable t) {
			throw new IllegalStateException("Could not load default ContextLoader class ["
					+ defaultContextLoaderClassName + "]. Specify @ContextConfiguration's 'loader' "
					+ "attribute or make the default loader class available.", t);
		}
	}

	/**
	 * Convenience method for creating a {@link ContextConfigurationAttributes} instance
	 * from the supplied {@link ContextConfiguration} and declaring class and then adding
	 * the attributes to the supplied list.
	 */
	private static void convertContextConfigToConfigAttributesAndAddToList(ContextConfiguration contextConfiguration,
			Class<?> declaringClass, final List<ContextConfigurationAttributes> attributesList) {
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Retrieved @ContextConfiguration [%s] for declaring class [%s].",
				contextConfiguration, declaringClass.getName()));
		}

		ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(declaringClass,
			contextConfiguration);
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved context configuration attributes: " + attributes);
		}
		attributesList.add(attributes);
	}

	/**
	 * Resolve the list of lists of {@linkplain ContextConfigurationAttributes context
	 * configuration attributes} for the supplied {@linkplain Class test class} and its
	 * superclasses, taking into account context hierarchies declared via
	 * {@link ContextHierarchy @ContextHierarchy} and
	 * {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * <p>The outer list represents a top-down ordering of context configuration
	 * attributes, where each element in the list represents the context configuration
	 * declared on a given test class in the class hierarchy. Each nested list
	 * contains the context configuration attributes declared either via a single
	 * instance of {@code @ContextConfiguration} on the particular class or via
	 * multiple instances of {@code @ContextConfiguration} declared within a
	 * single {@code @ContextHierarchy} instance on the particular class.
	 * Furthermore, each nested list maintains the order in which
	 * {@code @ContextConfiguration} instances are declared.
	 *
	 * <p>Note that the {@link ContextConfiguration#inheritLocations inheritLocations} and
	 * {@link ContextConfiguration#inheritInitializers() inheritInitializers} flags of
	 * {@link ContextConfiguration @ContextConfiguration} will <strong>not</strong>
	 * be taken into consideration. If these flags need to be honored, that must be
	 * handled manually when traversing the nested lists returned by this method.
	 *
	 * @param testClass the class for which to resolve the context hierarchy attributes
	 * (must not be {@code null})
	 * @return the list of lists of configuration attributes for the specified class;
	 * never {@code null}
	 * @throws IllegalArgumentException if the supplied class is {@code null}; if
	 * neither {@code @ContextConfiguration} nor {@code @ContextHierarchy} is
	 * <em>present</em> on the supplied class; or if a given class in the class hierarchy
	 * declares both {@code @ContextConfiguration} and {@code @ContextHierarchy} as
	 * top-level annotations.
	 *
	 * @since 3.2.2
	 * @see #buildContextHierarchyMap(Class)
	 * @see #resolveContextConfigurationAttributes(Class)
	 */
	static List<List<ContextConfigurationAttributes>> resolveContextHierarchyAttributes(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		final Class<ContextConfiguration> contextConfigType = ContextConfiguration.class;
		final Class<ContextHierarchy> contextHierarchyType = ContextHierarchy.class;
		final List<Class<? extends Annotation>> annotationTypes = Arrays.asList(contextConfigType, contextHierarchyType);

		final List<List<ContextConfigurationAttributes>> hierarchyAttributes = new ArrayList<List<ContextConfigurationAttributes>>();

		Class<?> declaringClass = findAnnotationDeclaringClassForTypes(annotationTypes, testClass);
		Assert.notNull(declaringClass, String.format(
			"Could not find an 'annotation declaring class' for annotation type [%s] or [%s] and test class [%s]",
			contextConfigType.getName(), contextHierarchyType.getName(), testClass.getName()));

		while (declaringClass != null) {

			boolean contextConfigDeclaredLocally = isAnnotationDeclaredLocally(contextConfigType, declaringClass);
			boolean contextHierarchyDeclaredLocally = isAnnotationDeclaredLocally(contextHierarchyType, declaringClass);

			if (contextConfigDeclaredLocally && contextHierarchyDeclaredLocally) {
				String msg = String.format("Test class [%s] has been configured with both @ContextConfiguration "
						+ "and @ContextHierarchy as class-level annotations. Only one of these annotations may "
						+ "be declared as a top-level annotation per test class.", declaringClass.getName());
				logger.error(msg);
				throw new IllegalStateException(msg);
			}

			final List<ContextConfigurationAttributes> configAttributesList = new ArrayList<ContextConfigurationAttributes>();

			if (contextConfigDeclaredLocally) {
				ContextConfiguration contextConfiguration = declaringClass.getAnnotation(contextConfigType);
				convertContextConfigToConfigAttributesAndAddToList(contextConfiguration, declaringClass,
					configAttributesList);
			}
			else if (contextHierarchyDeclaredLocally) {
				ContextHierarchy contextHierarchy = declaringClass.getAnnotation(contextHierarchyType);
				for (ContextConfiguration contextConfiguration : contextHierarchy.value()) {
					convertContextConfigToConfigAttributesAndAddToList(contextConfiguration, declaringClass,
						configAttributesList);
				}
			}
			else {
				// This should theoretically actually never happen...
				String msg = String.format("Test class [%s] has been configured with neither @ContextConfiguration "
						+ "nor @ContextHierarchy as a class-level annotation.", declaringClass.getName());
				logger.error(msg);
				throw new IllegalStateException(msg);
			}

			hierarchyAttributes.add(0, configAttributesList);

			declaringClass = findAnnotationDeclaringClassForTypes(annotationTypes, declaringClass.getSuperclass());
		}

		return hierarchyAttributes;
	}

	/**
	 * Build a <em>context hierarchy map</em> for the supplied {@linkplain Class
	 * test class} and its superclasses, taking into account context hierarchies
	 * declared via {@link ContextHierarchy @ContextHierarchy} and
	 * {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * <p>Each value in the map represents the consolidated list of {@linkplain
	 * ContextConfigurationAttributes context configuration attributes} for a
	 * given level in the context hierarchy (potentially across the test class
	 * hierarchy), keyed by the {@link ContextConfiguration#name() name} of the
	 * context hierarchy level.
	 *
	 * <p>If a given level in the context hierarchy does not have an explicit
	 * name (i.e., configured via {@link ContextConfiguration#name}), a name will
	 * be generated for that hierarchy level by appending the numerical level to
	 * the {@link #GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX}.
	 *
	 * @param testClass the class for which to resolve the context hierarchy map
	 * (must not be {@code null})
	 * @return a map of context configuration attributes for the context hierarchy,
	 * keyed by context hierarchy level name; never {@code null}
	 * @throws IllegalArgumentException if the lists of context configuration
	 * attributes for each level in the {@code @ContextHierarchy} do not define
	 * unique context configuration within the overall hierarchy.
	 *
	 * @since 3.2.2
	 * @see #resolveContextHierarchyAttributes(Class)
	 */
	static Map<String, List<ContextConfigurationAttributes>> buildContextHierarchyMap(Class<?> testClass) {
		final Map<String, List<ContextConfigurationAttributes>> map = new LinkedHashMap<String, List<ContextConfigurationAttributes>>();
		int hierarchyLevel = 1;

		for (List<ContextConfigurationAttributes> configAttributesList : resolveContextHierarchyAttributes(testClass)) {
			for (ContextConfigurationAttributes configAttributes : configAttributesList) {
				String name = configAttributes.getName();

				// Assign a generated name?
				if (!StringUtils.hasText(name)) {
					name = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + hierarchyLevel;
				}

				// Encountered a new context hierarchy level?
				if (!map.containsKey(name)) {
					hierarchyLevel++;
					map.put(name, new ArrayList<ContextConfigurationAttributes>());
				}

				map.get(name).add(configAttributes);
			}
		}

		// Check for uniqueness
		Set<List<ContextConfigurationAttributes>> set = new HashSet<List<ContextConfigurationAttributes>>(map.values());
		if (set.size() != map.size()) {
			String msg = String.format("The @ContextConfiguration elements configured via "
					+ "@ContextHierarchy in test class [%s] and its superclasses must "
					+ "define unique contexts per hierarchy level.", testClass.getName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}

		return map;
	}

	/**
	 * Resolve the list of {@linkplain ContextConfigurationAttributes context
	 * configuration attributes} for the supplied {@linkplain Class test class} and its
	 * superclasses.
	 *
	 * <p>Note that the {@link ContextConfiguration#inheritLocations inheritLocations} and
	 * {@link ContextConfiguration#inheritInitializers() inheritInitializers} flags of
	 * {@link ContextConfiguration @ContextConfiguration} will <strong>not</strong>
	 * be taken into consideration. If these flags need to be honored, that must be
	 * handled manually when traversing the list returned by this method.
	 *
	 * @param testClass the class for which to resolve the configuration attributes (must
	 * not be {@code null})
	 * @return the list of configuration attributes for the specified class, ordered
	 * <em>bottom-up</em> (i.e., as if we were traversing up the class hierarchy);
	 * never {@code null}
	 * @throws IllegalArgumentException if the supplied class is {@code null} or if
	 * {@code @ContextConfiguration} is not <em>present</em> on the supplied class
	 */
	static List<ContextConfigurationAttributes> resolveContextConfigurationAttributes(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		final List<ContextConfigurationAttributes> attributesList = new ArrayList<ContextConfigurationAttributes>();

		Class<ContextConfiguration> annotationType = ContextConfiguration.class;
		Class<?> declaringClass = findAnnotationDeclaringClass(annotationType, testClass);
		Assert.notNull(declaringClass, String.format(
			"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
			annotationType.getName(), testClass.getName()));

		while (declaringClass != null) {
			ContextConfiguration contextConfiguration = declaringClass.getAnnotation(annotationType);
			convertContextConfigToConfigAttributesAndAddToList(contextConfiguration, declaringClass, attributesList);
			declaringClass = findAnnotationDeclaringClass(annotationType, declaringClass.getSuperclass());
		}

		return attributesList;
	}

	/**
	 * Resolve the list of merged {@code ApplicationContextInitializer} classes for the
	 * supplied list of {@code ContextConfigurationAttributes}.
	 *
	 * <p>Note that the {@link ContextConfiguration#inheritInitializers inheritInitializers}
	 * flag of {@link ContextConfiguration @ContextConfiguration} will be taken into
	 * consideration. Specifically, if the {@code inheritInitializers} flag is set to
	 * {@code true} for a given level in the class hierarchy represented by the provided
	 * configuration attributes, context initializer classes defined at the given level
	 * will be merged with those defined in higher levels of the class hierarchy.
	 *
	 * @param configAttributesList the list of configuration attributes to process; must
	 * not be {@code null} or <em>empty</em>; must be ordered <em>bottom-up</em>
	 * (i.e., as if we were traversing up the class hierarchy)
	 * @return the set of merged context initializer classes, including those from
	 * superclasses if appropriate (never {@code null})
	 * @since 3.2
	 */
	static Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> resolveInitializerClasses(
			List<ContextConfigurationAttributes> configAttributesList) {
		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be empty");

		final Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses = //
		new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing context initializers for context configuration attributes %s",
					configAttributes));
			}

			initializerClasses.addAll(Arrays.asList(configAttributes.getInitializers()));

			if (!configAttributes.isInheritInitializers()) {
				break;
			}
		}

		return initializerClasses;
	}

	/**
	 * Resolve <em>active bean definition profiles</em> for the supplied {@link Class}.
	 *
	 * <p>Note that the {@link ActiveProfiles#inheritProfiles inheritProfiles} flag of
	 * {@link ActiveProfiles @ActiveProfiles} will be taken into consideration.
	 * Specifically, if the {@code inheritProfiles} flag is set to {@code true}, profiles
	 * defined in the test class will be merged with those defined in superclasses.
	 *
	 * @param testClass the class for which to resolve the active profiles (must not be
	 * {@code null})
	 * @return the set of active profiles for the specified class, including active
	 * profiles from superclasses if appropriate (never {@code null})
	 * @see ActiveProfiles
	 * @see org.springframework.context.annotation.Profile
	 */
	static String[] resolveActiveProfiles(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		Class<ActiveProfiles> annotationType = ActiveProfiles.class;
		Class<?> declaringClass = findAnnotationDeclaringClass(annotationType, testClass);

		if (declaringClass == null && logger.isDebugEnabled()) {
			logger.debug(String.format(
				"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
				annotationType.getName(), testClass.getName()));
		}

		final Set<String> activeProfiles = new HashSet<String>();

		while (declaringClass != null) {
			ActiveProfiles annotation = declaringClass.getAnnotation(annotationType);
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @ActiveProfiles [%s] for declaring class [%s].", annotation,
					declaringClass.getName()));
			}
			validateActiveProfilesConfiguration(declaringClass, annotation);

			String[] profiles = annotation.profiles();
			String[] valueProfiles = annotation.value();
			Class<? extends ActiveProfilesResolver> resolverClass = annotation.resolver();

			boolean resolverDeclared = !ActiveProfilesResolver.class.equals(resolverClass);
			boolean valueDeclared = !ObjectUtils.isEmpty(valueProfiles);

			if (resolverDeclared) {
				ActiveProfilesResolver resolver = null;
				try {
					resolver = instantiateClass(resolverClass, ActiveProfilesResolver.class);
				}
				catch (Exception e) {
					String msg = String.format("Could not instantiate ActiveProfilesResolver of "
							+ "type [%s] for test class [%s].", resolverClass.getName(), declaringClass.getName());
					logger.error(msg);
					throw new IllegalStateException(msg, e);
				}

				profiles = resolver.resolve(declaringClass);
				if (profiles == null) {
					String msg = String.format(
						"ActiveProfilesResolver [%s] returned a null array of bean definition profiles.",
						resolverClass.getName());
					logger.error(msg);
					throw new IllegalStateException(msg);
				}
			}
			else if (valueDeclared) {
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

	private static void validateActiveProfilesConfiguration(Class<?> declaringClass, ActiveProfiles annotation) {
		String[] valueProfiles = annotation.value();
		String[] profiles = annotation.profiles();
		Class<? extends ActiveProfilesResolver> resolverClass = annotation.resolver();
		boolean valueDeclared = !ObjectUtils.isEmpty(valueProfiles);
		boolean profilesDeclared = !ObjectUtils.isEmpty(profiles);
		boolean resolverDeclared = !ActiveProfilesResolver.class.equals(resolverClass);

		String msg = null;

		if (valueDeclared && profilesDeclared) {
			msg = String.format("Test class [%s] has been configured with @ActiveProfiles' 'value' [%s] "
					+ "and 'profiles' [%s] attributes. Only one declaration of active bean "
					+ "definition profiles is permitted per @ActiveProfiles annotation.", declaringClass.getName(),
				ObjectUtils.nullSafeToString(valueProfiles), ObjectUtils.nullSafeToString(profiles));
		}
		else if (valueDeclared && resolverDeclared) {
			msg = String.format("Test class [%s] has been configured with @ActiveProfiles' 'value' [%s] "
					+ "and 'resolver' [%s] attributes. Only one source of active bean "
					+ "definition profiles is permitted per @ActiveProfiles annotation, "
					+ "either declaritively or programmatically.", declaringClass.getName(),
				ObjectUtils.nullSafeToString(valueProfiles), resolverClass.getName());
		}
		else if (profilesDeclared && resolverDeclared) {
			msg = String.format("Test class [%s] has been configured with @ActiveProfiles' 'profiles' [%s] "
					+ "and 'resolver' [%s] attributes. Only one source of active bean "
					+ "definition profiles is permitted per @ActiveProfiles annotation, "
					+ "either declaritively or programmatically.", declaringClass.getName(),
				ObjectUtils.nullSafeToString(profiles), resolverClass.getName());
		}

		if (msg != null) {
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

	/**
	 * Build the {@link MergedContextConfiguration merged context configuration} for
	 * the supplied {@link Class testClass} and {@code defaultContextLoaderClassName},
	 * taking into account context hierarchies declared via
	 * {@link ContextHierarchy @ContextHierarchy} and
	 * {@link ContextConfiguration @ContextConfiguration}.
	 *
	 * @param testClass the test class for which the {@code MergedContextConfiguration}
	 * should be built (must not be {@code null})
	 * @param defaultContextLoaderClassName the name of the default {@code ContextLoader}
	 * class to use (may be {@code null})
	 * @param cacheAwareContextLoaderDelegate the cache-aware context loader delegate to
	 * be passed to the {@code MergedContextConfiguration} constructor
	 * @return the merged context configuration
	 * @see #buildContextHierarchyMap(Class)
	 * @see #buildMergedContextConfiguration(Class, List, String, MergedContextConfiguration, CacheAwareContextLoaderDelegate)
	 */
	@SuppressWarnings("javadoc")
	static MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass,
			String defaultContextLoaderClassName, CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

		if (testClass.isAnnotationPresent(ContextHierarchy.class)) {
			Map<String, List<ContextConfigurationAttributes>> hierarchyMap = buildContextHierarchyMap(testClass);

			MergedContextConfiguration parentConfig = null;
			MergedContextConfiguration mergedConfig = null;

			for (List<ContextConfigurationAttributes> list : hierarchyMap.values()) {
				List<ContextConfigurationAttributes> reversedList = new ArrayList<ContextConfigurationAttributes>(list);
				Collections.reverse(reversedList);

				// Don't use the supplied testClass; instead ensure that we are
				// building the MCC for the actual test class that declared the
				// configuration for the current level in the context hierarchy.
				Assert.notEmpty(reversedList, "ContextConfigurationAttributes list must not be empty");
				Class<?> declaringClass = reversedList.get(0).getDeclaringClass();

				mergedConfig = buildMergedContextConfiguration(declaringClass, reversedList,
					defaultContextLoaderClassName, parentConfig, cacheAwareContextLoaderDelegate);
				parentConfig = mergedConfig;
			}

			// Return the last level in the context hierarchy
			return mergedConfig;
		}
		else {
			return buildMergedContextConfiguration(testClass, resolveContextConfigurationAttributes(testClass),
				defaultContextLoaderClassName, null, cacheAwareContextLoaderDelegate);
		}
	}

	/**
	 * Build the {@link MergedContextConfiguration merged context configuration} for the
	 * supplied {@link Class testClass}, context configuration attributes,
	 * {@code defaultContextLoaderClassName}, and parent context configuration.
	 *
	 * @param testClass the test class for which the {@code MergedContextConfiguration}
	 * should be built (must not be {@code null})
	 * @param configAttributesList the list of context configuration attributes for the
	 * specified test class, ordered <em>bottom-up</em> (i.e., as if we were
	 * traversing up the class hierarchy); never {@code null} or empty
	 * @param defaultContextLoaderClassName the name of the default {@code ContextLoader}
	 * class to use (may be {@code null})
	 * @param parentConfig the merged context configuration for the parent application
	 * context in a context hierarchy, or {@code null} if there is no parent
	 * @param cacheAwareContextLoaderDelegate the cache-aware context loader delegate to
	 * be passed to the {@code MergedContextConfiguration} constructor
	 * @return the merged context configuration
	 * @see #resolveContextLoader
	 * @see #resolveContextConfigurationAttributes
	 * @see SmartContextLoader#processContextConfiguration
	 * @see ContextLoader#processLocations
	 * @see #resolveActiveProfiles
	 * @see MergedContextConfiguration
	 */
	private static MergedContextConfiguration buildMergedContextConfiguration(final Class<?> testClass,
			final List<ContextConfigurationAttributes> configAttributesList,
			final String defaultContextLoaderClassName, MergedContextConfiguration parentConfig,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

		final ContextLoader contextLoader = resolveContextLoader(testClass, configAttributesList,
			defaultContextLoaderClassName);
		final List<String> locationsList = new ArrayList<String>();
		final List<Class<?>> classesList = new ArrayList<Class<?>>();

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing locations and classes for context configuration attributes %s",
					configAttributes));
			}

			if (contextLoader instanceof SmartContextLoader) {
				SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
				smartContextLoader.processContextConfiguration(configAttributes);
				locationsList.addAll(0, Arrays.asList(configAttributes.getLocations()));
				classesList.addAll(0, Arrays.asList(configAttributes.getClasses()));
			}
			else {
				String[] processedLocations = contextLoader.processLocations(configAttributes.getDeclaringClass(),
					configAttributes.getLocations());
				locationsList.addAll(0, Arrays.asList(processedLocations));
				// Legacy ContextLoaders don't know how to process classes
			}

			if (!configAttributes.isInheritLocations()) {
				break;
			}
		}

		String[] locations = StringUtils.toStringArray(locationsList);
		Class<?>[] classes = ClassUtils.toClassArray(classesList);
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses = resolveInitializerClasses(configAttributesList);
		String[] activeProfiles = resolveActiveProfiles(testClass);

		MergedContextConfiguration mergedConfig = buildWebMergedContextConfiguration(testClass, locations, classes,
			initializerClasses, activeProfiles, contextLoader, cacheAwareContextLoaderDelegate, parentConfig);

		if (mergedConfig == null) {
			mergedConfig = new MergedContextConfiguration(testClass, locations, classes, initializerClasses,
				activeProfiles, contextLoader, cacheAwareContextLoaderDelegate, parentConfig);
		}

		return mergedConfig;
	}

	/**
	 * Load the {@link org.springframework.test.context.web.WebAppConfiguration}
	 * class, using reflection in order to avoid package cycles.
	 *
	 * @return the {@code @WebAppConfiguration} class or {@code null} if it cannot be loaded
	 * @since 3.2
	 */
	@SuppressWarnings("unchecked")
	private static Class<? extends Annotation> loadWebAppConfigurationClass() {
		Class<? extends Annotation> webAppConfigClass = null;
		try {
			webAppConfigClass = (Class<? extends Annotation>) ClassUtils.forName(WEB_APP_CONFIGURATION_CLASS_NAME,
				ContextLoaderUtils.class.getClassLoader());
		}
		catch (Throwable t) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not load @WebAppConfiguration class [" + WEB_APP_CONFIGURATION_CLASS_NAME + "].", t);
			}
		}
		return webAppConfigClass;
	}

	/**
	 * Attempt to build a {@link org.springframework.test.context.web.WebMergedContextConfiguration}
	 * from the supplied arguments, using reflection in order to avoid package cycles.
	 *
	 * @return the {@code WebMergedContextConfiguration} or {@code null} if it could not be built
	 * @since 3.2
	 */
	@SuppressWarnings("unchecked")
	private static MergedContextConfiguration buildWebMergedContextConfiguration(
			Class<?> testClass,
			String[] locations,
			Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses,
			String[] activeProfiles, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, MergedContextConfiguration parentConfig) {

		Class<? extends Annotation> webAppConfigClass = loadWebAppConfigurationClass();

		if (webAppConfigClass != null && testClass.isAnnotationPresent(webAppConfigClass)) {
			Annotation annotation = testClass.getAnnotation(webAppConfigClass);
			String resourceBasePath = (String) AnnotationUtils.getValue(annotation);

			try {
				Class<? extends MergedContextConfiguration> webMergedConfigClass = (Class<? extends MergedContextConfiguration>) ClassUtils.forName(
					WEB_MERGED_CONTEXT_CONFIGURATION_CLASS_NAME, ContextLoaderUtils.class.getClassLoader());

				Constructor<? extends MergedContextConfiguration> constructor = ClassUtils.getConstructorIfAvailable(
					webMergedConfigClass, Class.class, String[].class, Class[].class, Set.class, String[].class,
					String.class, ContextLoader.class, CacheAwareContextLoaderDelegate.class,
					MergedContextConfiguration.class);

				if (constructor != null) {
					return instantiateClass(constructor, testClass, locations, classes, initializerClasses,
						activeProfiles, resourceBasePath, contextLoader, cacheAwareContextLoaderDelegate, parentConfig);
				}
			}
			catch (Throwable t) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not instantiate [" + WEB_MERGED_CONTEXT_CONFIGURATION_CLASS_NAME + "].", t);
				}
			}
		}

		return null;
	}

}
