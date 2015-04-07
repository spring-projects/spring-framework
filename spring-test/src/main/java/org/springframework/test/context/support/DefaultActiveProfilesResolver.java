/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ActiveProfilesResolver;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.springframework.test.util.MetaAnnotationUtils.*;

/**
 * Default implementation of the {@link ActiveProfilesResolver} strategy that
 * resolves <em>active bean definition profiles</em> based solely on profiles
 * configured declaratively via {@link ActiveProfiles#profiles} or
 * {@link ActiveProfiles#value}.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see ActiveProfiles
 * @see ActiveProfilesResolver
 */
public class DefaultActiveProfilesResolver implements ActiveProfilesResolver {

	private static final Log logger = LogFactory.getLog(DefaultActiveProfilesResolver.class);


	/**
	 * Resolve the <em>bean definition profiles</em> for the given {@linkplain
	 * Class test class} based on profiles configured declaratively via
	 * {@link ActiveProfiles#profiles} or {@link ActiveProfiles#value}.
	 * @param testClass the test class for which the profiles should be resolved;
	 * never {@code null}
	 * @return the list of bean definition profiles to use when loading the
	 * {@code ApplicationContext}; never {@code null}
	 */
	@Override
	public String[] resolve(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		final Set<String> activeProfiles = new HashSet<String>();

		Class<ActiveProfiles> annotationType = ActiveProfiles.class;
		AnnotationDescriptor<ActiveProfiles> descriptor = findAnnotationDescriptor(testClass, annotationType);

		if (descriptor == null) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
					"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
					annotationType.getName(), testClass.getName()));
			}
		}
		else {
			Class<?> declaringClass = descriptor.getDeclaringClass();

			AnnotationAttributes annAttrs = descriptor.getAnnotationAttributes();
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @ActiveProfiles attributes [%s] for declaring class [%s].",
					annAttrs, declaringClass.getName()));
			}

			String[] profiles = annAttrs.getStringArray("profiles");
			String[] valueProfiles = annAttrs.getStringArray("value");
			boolean valueDeclared = !ObjectUtils.isEmpty(valueProfiles);
			boolean profilesDeclared = !ObjectUtils.isEmpty(profiles);

			if (valueDeclared && profilesDeclared) {
				String msg = String.format("Class [%s] has been configured with @ActiveProfiles' 'value' [%s] "
						+ "and 'profiles' [%s] attributes. Only one declaration of active bean "
						+ "definition profiles is permitted per @ActiveProfiles annotation.", declaringClass.getName(),
					ObjectUtils.nullSafeToString(valueProfiles), ObjectUtils.nullSafeToString(profiles));
				logger.error(msg);
				throw new IllegalStateException(msg);
			}

			if (valueDeclared) {
				profiles = valueProfiles;
			}

			for (String profile : profiles) {
				if (StringUtils.hasText(profile)) {
					activeProfiles.add(profile.trim());
				}
			}
		}

		return StringUtils.toStringArray(activeProfiles);
	}

}
