/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ActiveProfilesResolver;
import org.springframework.test.util.MetaAnnotationUtils;
import org.springframework.test.util.MetaAnnotationUtils.AnnotationDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility methods for working with {@link ActiveProfiles @ActiveProfiles} and
 * {@link ActiveProfilesResolver ActiveProfilesResolvers}.
 *
 * <p>Although {@code ActiveProfilesUtils} was first introduced in Spring Framework
 * 4.1, the initial implementations of methods in this class were based on the
 * existing code base in {@code ContextLoaderUtils}.
 *
 * @author Sam Brannen
 * @author Michail Nikolaev
 * @since 4.1
 * @see ActiveProfiles
 * @see ActiveProfilesResolver
 */
abstract class ActiveProfilesUtils {

	private static final Log logger = LogFactory.getLog(ActiveProfilesUtils.class);


	/**
	 * Resolve <em>active bean definition profiles</em> for the supplied {@link Class}.
	 * <p>Note that the {@link ActiveProfiles#inheritProfiles inheritProfiles} flag of
	 * {@link ActiveProfiles @ActiveProfiles} will be taken into consideration.
	 * Specifically, if the {@code inheritProfiles} flag is set to {@code true}, profiles
	 * defined in the test class will be merged with those defined in superclasses.
	 * @param testClass the class for which to resolve the active profiles (must not be
	 * {@code null})
	 * @return the set of active profiles for the specified class, including active
	 * profiles from superclasses if appropriate (never {@code null})
	 * @see ActiveProfiles
	 * @see ActiveProfilesResolver
	 * @see org.springframework.context.annotation.Profile
	 */
	static String[] resolveActiveProfiles(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		final List<String[]> profileArrays = new ArrayList<String[]>();

		Class<ActiveProfiles> annotationType = ActiveProfiles.class;
		AnnotationDescriptor<ActiveProfiles> descriptor =
				MetaAnnotationUtils.findAnnotationDescriptor(testClass, annotationType);
		if (descriptor == null && logger.isDebugEnabled()) {
			logger.debug(String.format(
					"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
					annotationType.getName(), testClass.getName()));
		}

		while (descriptor != null) {
			Class<?> rootDeclaringClass = descriptor.getRootDeclaringClass();
			Class<?> declaringClass = descriptor.getDeclaringClass();
			ActiveProfiles annotation = descriptor.synthesizeAnnotation();

			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @ActiveProfiles [%s] for declaring class [%s]",
						annotation, declaringClass.getName()));
			}

			Class<? extends ActiveProfilesResolver> resolverClass = annotation.resolver();
			if (ActiveProfilesResolver.class == resolverClass) {
				resolverClass = DefaultActiveProfilesResolver.class;
			}

			ActiveProfilesResolver resolver;
			try {
				resolver = BeanUtils.instantiateClass(resolverClass, ActiveProfilesResolver.class);
			}
			catch (Exception ex) {
				String msg = String.format("Could not instantiate ActiveProfilesResolver of type [%s] " +
						"for test class [%s]", resolverClass.getName(), rootDeclaringClass.getName());
				logger.error(msg);
				throw new IllegalStateException(msg, ex);
			}

			String[] profiles = resolver.resolve(rootDeclaringClass);
			if (profiles == null) {
				String msg = String.format(
						"ActiveProfilesResolver [%s] returned a null array of bean definition profiles",
						resolverClass.getName());
				logger.error(msg);
				throw new IllegalStateException(msg);
			}

			profileArrays.add(profiles);

			descriptor = (annotation.inheritProfiles() ? MetaAnnotationUtils.findAnnotationDescriptor(
					rootDeclaringClass.getSuperclass(), annotationType) : null);
		}

		// Reverse the list so that we can traverse "down" the hierarchy.
		Collections.reverse(profileArrays);

		final Set<String> activeProfiles = new LinkedHashSet<String>();
		for (String[] profiles : profileArrays) {
			for (String profile : profiles) {
				if (StringUtils.hasText(profile)) {
					activeProfiles.add(profile.trim());
				}
			}
		}

		return StringUtils.toStringArray(activeProfiles);
	}

}
