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

package org.springframework.test.context.support;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.Assert;

/**
 * Utility methods for {@link SmartContextLoader SmartContextLoaders} that deal
 * with annotated classes (e.g., {@link Configuration @Configuration} classes).
 *
 * @author Sam Brannen
 * @since 3.2
 */
public abstract class AnnotationConfigContextLoaderUtils {

	private static final Log logger = LogFactory.getLog(AnnotationConfigContextLoaderUtils.class);


	private AnnotationConfigContextLoaderUtils() {
		/* no-op */
	}

	private static boolean isStaticNonPrivateAndNonFinal(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		int modifiers = clazz.getModifiers();
		return (Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers) && !Modifier.isFinal(modifiers));
	}

	/**
	 * Determine if the supplied {@link Class} meets the criteria for being
	 * considered a <em>default configuration class</em> candidate.
	 *
	 * <p>Specifically, such candidates:
	 *
	 * <ul>
	 * <li>must not be <code>null</code></li>
	 * <li>must not be <code>private</code></li>
	 * <li>must not be <code>final</code></li>
	 * <li>must be <code>static</code></li>
	 * <li>must be annotated with {@code @Configuration}</li>
	 * </ul>
	 *
	 * @param clazz the class to check
	 * @return <code>true</code> if the supplied class meets the candidate criteria
	 */
	private static boolean isDefaultConfigurationClassCandidate(Class<?> clazz) {
		return clazz != null && isStaticNonPrivateAndNonFinal(clazz) && clazz.isAnnotationPresent(Configuration.class);
	}

	/**
	 * Detect the default configuration classes for the supplied test class.
	 *
	 * <p>The returned class array will contain all static inner classes of
	 * the supplied class that meet the requirements for {@code @Configuration}
	 * class implementations as specified in the documentation for
	 * {@link Configuration @Configuration}.
	 *
	 * <p>The implementation of this method adheres to the contract defined in the
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader}
	 * SPI. Specifically, this method uses introspection to detect default
	 * configuration classes that comply with the constraints required of
	 * {@code @Configuration} class implementations. If a potential candidate
	 * configuration class does not meet these requirements, this method will log a
	 * warning, and the potential candidate class will be ignored.
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @return an array of default configuration classes, potentially empty but
	 * never <code>null</code>
	 */
	public static Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
		Assert.notNull(declaringClass, "Declaring class must not be null");

		List<Class<?>> configClasses = new ArrayList<Class<?>>();

		for (Class<?> candidate : declaringClass.getDeclaredClasses()) {
			if (isDefaultConfigurationClassCandidate(candidate)) {
				configClasses.add(candidate);
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
						"Ignoring class [%s]; it must be static, non-private, non-final, and annotated "
								+ "with @Configuration to be considered a default configuration class.",
						candidate.getName()));
				}
			}
		}

		if (configClasses.isEmpty()) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Could not detect default configuration classes for test class [%s]: "
						+ "%s does not declare any static, non-private, non-final, inner classes "
						+ "annotated with @Configuration.", declaringClass.getName(), declaringClass.getSimpleName()));
			}
		}

		return configClasses.toArray(new Class<?>[configClasses.size()]);
	}

}
