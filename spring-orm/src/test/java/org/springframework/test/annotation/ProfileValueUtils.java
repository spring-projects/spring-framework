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

package org.springframework.test.annotation;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * General utility methods for working with <em>profile values</em>.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see ProfileValueSource
 * @see ProfileValueSourceConfiguration
 * @see IfProfileValue
 */
public abstract class ProfileValueUtils {

	private static final Log logger = LogFactory.getLog(ProfileValueUtils.class);


	/**
	 * Retrieves the {@link ProfileValueSource} type for the specified
	 * {@link Class test class} as configured via the
	 * {@link ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}
	 * annotation and instantiates a new instance of that type.
	 * <p>
	 * If
	 * {@link ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}
	 * is not present on the specified class or if a custom
	 * {@link ProfileValueSource} is not declared, the default
	 * {@link SystemProfileValueSource} will be returned instead.
	 *
	 * @param testClass The test class for which the ProfileValueSource should
	 * be retrieved
	 * @return the configured (or default) ProfileValueSource for the specified
	 * class
	 * @see SystemProfileValueSource
	 */
	@SuppressWarnings("unchecked")
	public static ProfileValueSource retrieveProfileValueSource(Class<?> testClass) {
		Assert.notNull(testClass, "testClass must not be null");

		Class<ProfileValueSourceConfiguration> annotationType = ProfileValueSourceConfiguration.class;
		ProfileValueSourceConfiguration config = testClass.getAnnotation(annotationType);
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieved @ProfileValueSourceConfiguration [" + config + "] for test class ["
					+ testClass.getName() + "]");
		}

		Class<? extends ProfileValueSource> profileValueSourceType;
		if (config != null) {
			profileValueSourceType = config.value();
		}
		else {
			profileValueSourceType = (Class<? extends ProfileValueSource>) AnnotationUtils.getDefaultValue(annotationType);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieved ProfileValueSource type [" + profileValueSourceType + "] for class ["
					+ testClass.getName() + "]");
		}

		ProfileValueSource profileValueSource;
		if (SystemProfileValueSource.class.equals(profileValueSourceType)) {
			profileValueSource = SystemProfileValueSource.getInstance();
		}
		else {
			try {
				profileValueSource = profileValueSourceType.newInstance();
			}
			catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("Could not instantiate a ProfileValueSource of type [" + profileValueSourceType
							+ "] for class [" + testClass.getName() + "]: using default.", e);
				}
				profileValueSource = SystemProfileValueSource.getInstance();
			}
		}

		return profileValueSource;
	}

	/**
	 * Determine if the supplied <code>testClass</code> is <em>enabled</em>
	 * in the current environment, as specified by the
	 * {@link IfProfileValue @IfProfileValue} annotation at the class level.
	 * <p>
	 * Defaults to <code>true</code> if no
	 * {@link IfProfileValue @IfProfileValue} annotation is declared.
	 *
	 * @param testClass the test class
	 * @return <code>true</code> if the test is <em>enabled</em> in the
	 * current environment
	 */
	public static boolean isTestEnabledInThisEnvironment(Class<?> testClass) {
		IfProfileValue ifProfileValue = testClass.getAnnotation(IfProfileValue.class);
		if (ifProfileValue == null) {
			return true;
		}
		ProfileValueSource profileValueSource = retrieveProfileValueSource(testClass);
		return isTestEnabledInThisEnvironment(profileValueSource, ifProfileValue);
	}

	/**
	 * Determine if the supplied <code>testMethod</code> is <em>enabled</em>
	 * in the current environment, as specified by the
	 * {@link IfProfileValue @IfProfileValue} annotation, which may be declared
	 * on the test method itself or at the class level.
	 * <p>
	 * Defaults to <code>true</code> if no
	 * {@link IfProfileValue @IfProfileValue} annotation is declared.
	 *
	 * @param testMethod the test method
	 * @param testClass the test class
	 * @return <code>true</code> if the test is <em>enabled</em> in the
	 * current environment
	 */
	public static boolean isTestEnabledInThisEnvironment(Method testMethod, Class<?> testClass) {
		IfProfileValue ifProfileValue = testMethod.getAnnotation(IfProfileValue.class);
		if (ifProfileValue == null) {
			ifProfileValue = testClass.getAnnotation(IfProfileValue.class);
			if (ifProfileValue == null) {
				return true;
			}
		}
		ProfileValueSource profileValueSource = retrieveProfileValueSource(testClass);
		return isTestEnabledInThisEnvironment(profileValueSource, ifProfileValue);
	}

	/**
	 * Determine if the supplied <code>testMethod</code> is <em>enabled</em>
	 * in the current environment, as specified by the
	 * {@link IfProfileValue @IfProfileValue} annotation, which may be declared
	 * on the test method itself or at the class level.
	 * <p>
	 * Defaults to <code>true</code> if no
	 * {@link IfProfileValue @IfProfileValue} annotation is declared.
	 *
	 * @param profileValueSource the ProfileValueSource to use to determine if
	 * the test is enabled
	 * @param testMethod the test method
	 * @param testClass the test class
	 * @return <code>true</code> if the test is <em>enabled</em> in the
	 * current environment
	 */
	public static boolean isTestEnabledInThisEnvironment(ProfileValueSource profileValueSource, Method testMethod,
			Class<?> testClass) {

		IfProfileValue ifProfileValue = testMethod.getAnnotation(IfProfileValue.class);
		if (ifProfileValue == null) {
			ifProfileValue = testClass.getAnnotation(IfProfileValue.class);
			if (ifProfileValue == null) {
				return true;
			}
		}
		return isTestEnabledInThisEnvironment(profileValueSource, ifProfileValue);
	}

	/**
	 * Determine if the <code>value</code> (or one of the <code>values</code>)
	 * in the supplied {@link IfProfileValue @IfProfileValue} annotation is
	 * <em>enabled</em> in the current environment.
	 *
	 * @param profileValueSource the ProfileValueSource to use to determine if
	 * the test is enabled
	 * @param ifProfileValue the annotation to introspect
	 * @return <code>true</code> if the test is <em>enabled</em> in the
	 * current environment
	 */
	private static boolean isTestEnabledInThisEnvironment(ProfileValueSource profileValueSource,
			IfProfileValue ifProfileValue) {

		String environmentValue = profileValueSource.get(ifProfileValue.name());
		String[] annotatedValues = ifProfileValue.values();
		if (StringUtils.hasLength(ifProfileValue.value())) {
			if (annotatedValues.length > 0) {
				throw new IllegalArgumentException("Setting both the 'value' and 'values' attributes "
						+ "of @IfProfileValue is not allowed: choose one or the other.");
			}
			annotatedValues = new String[] { ifProfileValue.value() };
		}

		for (String value : annotatedValues) {
			if (ObjectUtils.nullSafeEquals(value, environmentValue)) {
				return true;
			}
		}
		return false;
	}

}
