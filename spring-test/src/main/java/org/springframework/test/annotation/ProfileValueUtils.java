/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.annotation;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
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
	 * {@link ProfileValueSourceConfiguration
	 * &#064;ProfileValueSourceConfiguration} annotation and instantiates a new
	 * instance of that type.
	 * <p>If {@link ProfileValueSourceConfiguration
	 * &#064;ProfileValueSourceConfiguration} is not present on the specified
	 * class or if a custom {@link ProfileValueSource} is not declared, the
	 * default {@link SystemProfileValueSource} will be returned instead.
	 * @param testClass the test class for which the ProfileValueSource should
	 * be retrieved
	 * @return the configured (or default) ProfileValueSource for the specified
	 * class
	 * @see SystemProfileValueSource
	 */
	@SuppressWarnings("unchecked")
	public static ProfileValueSource retrieveProfileValueSource(Class<?> testClass) {
		Assert.notNull(testClass, "testClass must not be null");

		Class<ProfileValueSourceConfiguration> annotationType = ProfileValueSourceConfiguration.class;
		ProfileValueSourceConfiguration config = AnnotatedElementUtils.findMergedAnnotation(testClass, annotationType);
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieved @ProfileValueSourceConfiguration [" + config + "] for test class [" +
					testClass.getName() + "]");
		}

		Class<? extends ProfileValueSource> profileValueSourceType;
		if (config != null) {
			profileValueSourceType = config.value();
		}
		else {
			profileValueSourceType = (Class<? extends ProfileValueSource>) AnnotationUtils.getDefaultValue(annotationType);
			Assert.state(profileValueSourceType != null, "No default ProfileValueSource class");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieved ProfileValueSource type [" + profileValueSourceType + "] for class [" +
					testClass.getName() + "]");
		}

		ProfileValueSource profileValueSource;
		if (SystemProfileValueSource.class == profileValueSourceType) {
			profileValueSource = SystemProfileValueSource.getInstance();
		}
		else {
			try {
				profileValueSource = ReflectionUtils.accessibleConstructor(profileValueSourceType).newInstance();
			}
			catch (Exception ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Could not instantiate a ProfileValueSource of type [" + profileValueSourceType +
							"] for class [" + testClass.getName() + "]: using default.", ex);
				}
				profileValueSource = SystemProfileValueSource.getInstance();
			}
		}

		return profileValueSource;
	}

	/**
	 * Determine if the supplied {@code testClass} is <em>enabled</em> in
	 * the current environment, as specified by the {@link IfProfileValue
	 * &#064;IfProfileValue} annotation at the class level.
	 * <p>Defaults to {@code true} if no {@link IfProfileValue
	 * &#064;IfProfileValue} annotation is declared.
	 * @param testClass the test class
	 * @return {@code true} if the test is <em>enabled</em> in the current
	 * environment
	 */
	public static boolean isTestEnabledInThisEnvironment(Class<?> testClass) {
		IfProfileValue ifProfileValue = AnnotatedElementUtils.findMergedAnnotation(testClass, IfProfileValue.class);
		return isTestEnabledInThisEnvironment(retrieveProfileValueSource(testClass), ifProfileValue);
	}

	/**
	 * Determine if the supplied {@code testMethod} is <em>enabled</em> in
	 * the current environment, as specified by the {@link IfProfileValue
	 * &#064;IfProfileValue} annotation, which may be declared on the test
	 * method itself or at the class level. Class-level usage overrides
	 * method-level usage.
	 * <p>Defaults to {@code true} if no {@link IfProfileValue
	 * &#064;IfProfileValue} annotation is declared.
	 * @param testMethod the test method
	 * @param testClass the test class
	 * @return {@code true} if the test is <em>enabled</em> in the current
	 * environment
	 */
	public static boolean isTestEnabledInThisEnvironment(Method testMethod, Class<?> testClass) {
		return isTestEnabledInThisEnvironment(retrieveProfileValueSource(testClass), testMethod, testClass);
	}

	/**
	 * Determine if the supplied {@code testMethod} is <em>enabled</em> in
	 * the current environment, as specified by the {@link IfProfileValue
	 * &#064;IfProfileValue} annotation, which may be declared on the test
	 * method itself or at the class level. Class-level usage overrides
	 * method-level usage.
	 * <p>Defaults to {@code true} if no {@link IfProfileValue
	 * &#064;IfProfileValue} annotation is declared.
	 * @param profileValueSource the ProfileValueSource to use to determine if
	 * the test is enabled
	 * @param testMethod the test method
	 * @param testClass the test class
	 * @return {@code true} if the test is <em>enabled</em> in the current
	 * environment
	 */
	public static boolean isTestEnabledInThisEnvironment(ProfileValueSource profileValueSource, Method testMethod,
			Class<?> testClass) {

		IfProfileValue ifProfileValue = AnnotatedElementUtils.findMergedAnnotation(testClass, IfProfileValue.class);
		boolean classLevelEnabled = isTestEnabledInThisEnvironment(profileValueSource, ifProfileValue);

		if (classLevelEnabled) {
			ifProfileValue = AnnotatedElementUtils.findMergedAnnotation(testMethod, IfProfileValue.class);
			return isTestEnabledInThisEnvironment(profileValueSource, ifProfileValue);
		}

		return false;
	}

	/**
	 * Determine if the {@code value} (or one of the {@code values})
	 * in the supplied {@link IfProfileValue &#064;IfProfileValue} annotation is
	 * <em>enabled</em> in the current environment.
	 * @param profileValueSource the ProfileValueSource to use to determine if
	 * the test is enabled
	 * @param ifProfileValue the annotation to introspect; may be
	 * {@code null}
	 * @return {@code true} if the test is <em>enabled</em> in the current
	 * environment or if the supplied {@code ifProfileValue} is
	 * {@code null}
	 */
	private static boolean isTestEnabledInThisEnvironment(ProfileValueSource profileValueSource,
			@Nullable IfProfileValue ifProfileValue) {

		if (ifProfileValue == null) {
			return true;
		}

		String environmentValue = profileValueSource.get(ifProfileValue.name());
		String[] annotatedValues = ifProfileValue.values();
		if (StringUtils.hasLength(ifProfileValue.value())) {
			Assert.isTrue(annotatedValues.length == 0, "Setting both the 'value' and 'values' attributes " +
						"of @IfProfileValue is not allowed: choose one or the other.");
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
