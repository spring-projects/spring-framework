/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.util.MetaAnnotationUtils.AnnotationDescriptor;
import org.springframework.test.util.MetaAnnotationUtils.UntypedAnnotationDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.springframework.core.annotation.AnnotationUtils.getAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.isAnnotationDeclaredLocally;
import static org.springframework.test.util.MetaAnnotationUtils.findAnnotationDescriptor;
import static org.springframework.test.util.MetaAnnotationUtils.findAnnotationDescriptorForTypes;

/**
 * Utility methods for resolving {@link ContextConfigurationAttributes} from the
 * {@link ContextConfiguration @ContextConfiguration} and
 * {@link ContextHierarchy @ContextHierarchy} annotations for use with
 * {@link SmartContextLoader SmartContextLoaders}.
 *
 * @author Sam Brannen
 * @since 3.1
 * @see SmartContextLoader
 * @see ContextConfigurationAttributes
 * @see ContextConfiguration
 * @see ContextHierarchy
 */
abstract class ContextLoaderUtils {

	static final String GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX = "ContextHierarchyLevel#";

	private static final Log logger = LogFactory.getLog(ContextLoaderUtils.class);


	/**
	 * Resolve the list of lists of {@linkplain ContextConfigurationAttributes context
	 * configuration attributes} for the supplied {@linkplain Class test class} and its
	 * superclasses, taking into account context hierarchies declared via
	 * {@link ContextHierarchy @ContextHierarchy} and
	 * {@link ContextConfiguration @ContextConfiguration}.
	 * <p>The outer list represents a top-down ordering of context configuration
	 * attributes, where each element in the list represents the context configuration
	 * declared on a given test class in the class hierarchy. Each nested list
	 * contains the context configuration attributes declared either via a single
	 * instance of {@code @ContextConfiguration} on the particular class or via
	 * multiple instances of {@code @ContextConfiguration} declared within a
	 * single {@code @ContextHierarchy} instance on the particular class.
	 * Furthermore, each nested list maintains the order in which
	 * {@code @ContextConfiguration} instances are declared.
	 * <p>Note that the {@link ContextConfiguration#inheritLocations inheritLocations} and
	 * {@link ContextConfiguration#inheritInitializers() inheritInitializers} flags of
	 * {@link ContextConfiguration @ContextConfiguration} will <strong>not</strong>
	 * be taken into consideration. If these flags need to be honored, that must be
	 * handled manually when traversing the nested lists returned by this method.
	 * @param testClass the class for which to resolve the context hierarchy attributes
	 * (must not be {@code null})
	 * @return the list of lists of configuration attributes for the specified class;
	 * never {@code null}
	 * @throws IllegalArgumentException if the supplied class is {@code null}; or if
	 * neither {@code @ContextConfiguration} nor {@code @ContextHierarchy} is
	 * <em>present</em> on the supplied class
	 * @throws IllegalStateException if a test class or composed annotation
	 * in the class hierarchy declares both {@code @ContextConfiguration} and
	 * {@code @ContextHierarchy} as top-level annotations.
	 * @since 3.2.2
	 * @see #buildContextHierarchyMap(Class)
	 * @see #resolveContextConfigurationAttributes(Class)
	 */
	@SuppressWarnings("unchecked")
	static List<List<ContextConfigurationAttributes>> resolveContextHierarchyAttributes(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		Class<ContextConfiguration> contextConfigType = ContextConfiguration.class;
		Class<ContextHierarchy> contextHierarchyType = ContextHierarchy.class;
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = new ArrayList<>();

		UntypedAnnotationDescriptor desc =
				findAnnotationDescriptorForTypes(testClass, contextConfigType, contextHierarchyType);
		Assert.notNull(desc, () -> String.format(
					"Could not find an 'annotation declaring class' for annotation type [%s] or [%s] and test class [%s]",
					contextConfigType.getName(), contextHierarchyType.getName(), testClass.getName()));

		while (desc != null) {
			Class<?> rootDeclaringClass = desc.getRootDeclaringClass();
			Class<?> declaringClass = desc.getDeclaringClass();

			boolean contextConfigDeclaredLocally = isAnnotationDeclaredLocally(contextConfigType, declaringClass);
			boolean contextHierarchyDeclaredLocally = isAnnotationDeclaredLocally(contextHierarchyType, declaringClass);

			if (contextConfigDeclaredLocally && contextHierarchyDeclaredLocally) {
				String msg = String.format("Class [%s] has been configured with both @ContextConfiguration " +
						"and @ContextHierarchy. Only one of these annotations may be declared on a test class " +
						"or composed annotation.", declaringClass.getName());
				logger.error(msg);
				throw new IllegalStateException(msg);
			}

			List<ContextConfigurationAttributes> configAttributesList = new ArrayList<>();

			if (contextConfigDeclaredLocally) {
				ContextConfiguration contextConfiguration = AnnotationUtils.synthesizeAnnotation(
						desc.getAnnotationAttributes(), ContextConfiguration.class, desc.getRootDeclaringClass());
				convertContextConfigToConfigAttributesAndAddToList(
						contextConfiguration, rootDeclaringClass, configAttributesList);
			}
			else if (contextHierarchyDeclaredLocally) {
				ContextHierarchy contextHierarchy = getAnnotation(declaringClass, contextHierarchyType);
				if (contextHierarchy != null) {
					for (ContextConfiguration contextConfiguration : contextHierarchy.value()) {
						convertContextConfigToConfigAttributesAndAddToList(
								contextConfiguration, rootDeclaringClass, configAttributesList);
					}
				}
			}
			else {
				// This should theoretically never happen...
				String msg = String.format("Test class [%s] has been configured with neither @ContextConfiguration " +
						"nor @ContextHierarchy as a class-level annotation.", rootDeclaringClass.getName());
				logger.error(msg);
				throw new IllegalStateException(msg);
			}

			hierarchyAttributes.add(0, configAttributesList);
			desc = findAnnotationDescriptorForTypes(
					rootDeclaringClass.getSuperclass(), contextConfigType, contextHierarchyType);
		}

		return hierarchyAttributes;
	}

	/**
	 * Build a <em>context hierarchy map</em> for the supplied {@linkplain Class
	 * test class} and its superclasses, taking into account context hierarchies
	 * declared via {@link ContextHierarchy @ContextHierarchy} and
	 * {@link ContextConfiguration @ContextConfiguration}.
	 * <p>Each value in the map represents the consolidated list of {@linkplain
	 * ContextConfigurationAttributes context configuration attributes} for a
	 * given level in the context hierarchy (potentially across the test class
	 * hierarchy), keyed by the {@link ContextConfiguration#name() name} of the
	 * context hierarchy level.
	 * <p>If a given level in the context hierarchy does not have an explicit
	 * name (i.e., configured via {@link ContextConfiguration#name}), a name will
	 * be generated for that hierarchy level by appending the numerical level to
	 * the {@link #GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX}.
	 * @param testClass the class for which to resolve the context hierarchy map
	 * (must not be {@code null})
	 * @return a map of context configuration attributes for the context hierarchy,
	 * keyed by context hierarchy level name; never {@code null}
	 * @throws IllegalArgumentException if the lists of context configuration
	 * attributes for each level in the {@code @ContextHierarchy} do not define
	 * unique context configuration within the overall hierarchy.
	 * @since 3.2.2
	 * @see #resolveContextHierarchyAttributes(Class)
	 */
	static Map<String, List<ContextConfigurationAttributes>> buildContextHierarchyMap(Class<?> testClass) {
		final Map<String, List<ContextConfigurationAttributes>> map = new LinkedHashMap<>();
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
					map.put(name, new ArrayList<>());
				}

				map.get(name).add(configAttributes);
			}
		}

		// Check for uniqueness
		Set<List<ContextConfigurationAttributes>> set = new HashSet<>(map.values());
		if (set.size() != map.size()) {
			String msg = String.format("The @ContextConfiguration elements configured via @ContextHierarchy in " +
					"test class [%s] and its superclasses must define unique contexts per hierarchy level.",
					testClass.getName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}

		return map;
	}

	/**
	 * Resolve the list of {@linkplain ContextConfigurationAttributes context
	 * configuration attributes} for the supplied {@linkplain Class test class} and its
	 * superclasses.
	 * <p>Note that the {@link ContextConfiguration#inheritLocations inheritLocations} and
	 * {@link ContextConfiguration#inheritInitializers() inheritInitializers} flags of
	 * {@link ContextConfiguration @ContextConfiguration} will <strong>not</strong>
	 * be taken into consideration. If these flags need to be honored, that must be
	 * handled manually when traversing the list returned by this method.
	 * @param testClass the class for which to resolve the configuration attributes
	 * (must not be {@code null})
	 * @return the list of configuration attributes for the specified class, ordered
	 * <em>bottom-up</em> (i.e., as if we were traversing up the class hierarchy);
	 * never {@code null}
	 * @throws IllegalArgumentException if the supplied class is {@code null} or if
	 * {@code @ContextConfiguration} is not <em>present</em> on the supplied class
	 */
	static List<ContextConfigurationAttributes> resolveContextConfigurationAttributes(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		List<ContextConfigurationAttributes> attributesList = new ArrayList<>();
		Class<ContextConfiguration> annotationType = ContextConfiguration.class;

		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(testClass, annotationType);
		Assert.notNull(descriptor, () -> String.format(
					"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
					annotationType.getName(), testClass.getName()));

		while (descriptor != null) {
			convertContextConfigToConfigAttributesAndAddToList(descriptor.synthesizeAnnotation(),
					descriptor.getRootDeclaringClass(), attributesList);
			descriptor = findAnnotationDescriptor(descriptor.getRootDeclaringClass().getSuperclass(), annotationType);
		}

		return attributesList;
	}

	/**
	 * Convenience method for creating a {@link ContextConfigurationAttributes}
	 * instance from the supplied {@link ContextConfiguration} annotation and
	 * declaring class and then adding the attributes to the supplied list.
	 */
	private static void convertContextConfigToConfigAttributesAndAddToList(ContextConfiguration contextConfiguration,
			Class<?> declaringClass, final List<ContextConfigurationAttributes> attributesList) {

		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Retrieved @ContextConfiguration [%s] for declaring class [%s].",
					contextConfiguration, declaringClass.getName()));
		}
		ContextConfigurationAttributes attributes =
				new ContextConfigurationAttributes(declaringClass, contextConfiguration);
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved context configuration attributes: " + attributes);
		}
		attributesList.add(attributes);
	}

}
