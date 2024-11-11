/*
 * Copyright 2002-2024 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Utility methods for working with {@link TestConstructor @TestConstructor}.
 *
 * <p>Primarily intended for use within the framework.
 *
 * @author Sam Brannen
 * @author Florian Lehmann
 * @since 5.2
 * @see TestConstructor
 */
@SuppressWarnings("unchecked")
public abstract class TestConstructorUtils {

	private static final Log logger = LogFactory.getLog(TestConstructorUtils.class);

	private static final Set<Class<? extends Annotation>> autowiredAnnotationTypes = CollectionUtils.newLinkedHashSet(2);

	static {
		autowiredAnnotationTypes.add(Autowired.class);

		ClassLoader classLoader = TestConstructorUtils.class.getClassLoader();
		try {
			autowiredAnnotationTypes.add((Class<? extends Annotation>)
					ClassUtils.forName("jakarta.inject.Inject", classLoader));
			logger.trace("'jakarta.inject.Inject' annotation found and supported for autowiring");
		}
		catch (ClassNotFoundException ex) {
			// jakarta.inject API not available - simply skip.
		}

		try {
			autowiredAnnotationTypes.add((Class<? extends Annotation>)
					ClassUtils.forName("javax.inject.Inject", classLoader));
			logger.trace("'javax.inject.Inject' annotation found and supported for autowiring");
		}
		catch (ClassNotFoundException ex) {
			// javax.inject API not available - simply skip.
		}
	}


	private TestConstructorUtils() {
	}

	/**
	 * Determine if the supplied executable for the given test class is an
	 * autowirable constructor.
	 * <p>This method delegates to {@link #isAutowirableConstructor(Executable, Class, PropertyProvider)}
	 * will a value of {@code null} for the fallback {@link PropertyProvider}.
	 * @param executable an executable for the test class
	 * @param testClass the test class
	 * @return {@code true} if the executable is an autowirable constructor
	 * @see #isAutowirableConstructor(Executable, Class, PropertyProvider)
	 */
	public static boolean isAutowirableConstructor(Executable executable, Class<?> testClass) {
		return isAutowirableConstructor(executable, testClass, null);
	}

	/**
	 * Determine if the supplied constructor for the given test class is
	 * autowirable.
	 * <p>This method delegates to {@link #isAutowirableConstructor(Constructor, Class, PropertyProvider)}
	 * will a value of {@code null} for the fallback {@link PropertyProvider}.
	 * @param constructor a constructor for the test class
	 * @param testClass the test class
	 * @return {@code true} if the constructor is autowirable
	 * @see #isAutowirableConstructor(Constructor, Class, PropertyProvider)
	 */
	public static boolean isAutowirableConstructor(Constructor<?> constructor, Class<?> testClass) {
		return isAutowirableConstructor(constructor, testClass, null);
	}

	/**
	 * Determine if the supplied executable for the given test class is an
	 * autowirable constructor.
	 * <p>This method delegates to {@link #isAutowirableConstructor(Constructor, Class, PropertyProvider)}
	 * if the supplied executable is a constructor and otherwise returns {@code false}.
	 * @param executable an executable for the test class
	 * @param testClass the test class
	 * @param fallbackPropertyProvider fallback property provider used to look up
	 * the value for {@link TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME}
	 * if no such value is found in {@link SpringProperties}
	 * @return {@code true} if the executable is an autowirable constructor
	 * @since 5.3
	 * @see #isAutowirableConstructor(Constructor, Class, PropertyProvider)
	 */
	public static boolean isAutowirableConstructor(Executable executable, Class<?> testClass,
			@Nullable PropertyProvider fallbackPropertyProvider) {

		return (executable instanceof Constructor<?> constructor &&
				isAutowirableConstructor(constructor, testClass, fallbackPropertyProvider));
	}

	/**
	 * Determine if the supplied constructor for the given test class is
	 * autowirable.
	 *
	 * <p>A constructor is considered to be autowirable if one of the following
	 * conditions is {@code true}.
	 *
	 * <ol>
	 * <li>The constructor is annotated with {@link Autowired @Autowired},
	 * {@link jakarta.inject.Inject @jakarta.inject.Inject}, or
	 * {@link javax.inject.Inject @javax.inject.Inject}.</li>
	 * <li>{@link TestConstructor @TestConstructor} is <em>present</em> or
	 * <em>meta-present</em> on the test class with
	 * {@link TestConstructor#autowireMode() autowireMode} set to
	 * {@link AutowireMode#ALL ALL}.</li>
	 * <li>The default <em>test constructor autowire mode</em> has been set to
	 * {@code ALL} in {@link SpringProperties} or in the supplied fallback
	 * {@link PropertyProvider} (see
	 * {@link TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME}).</li>
	 * </ol>
	 * @param constructor a constructor for the test class
	 * @param testClass the test class
	 * @param fallbackPropertyProvider fallback property provider used to look up
	 * the value for the default <em>test constructor autowire mode</em> if no
	 * such value is found in {@link SpringProperties}
	 * @return {@code true} if the constructor is autowirable
	 * @since 5.3
	 */
	public static boolean isAutowirableConstructor(Constructor<?> constructor, Class<?> testClass,
			@Nullable PropertyProvider fallbackPropertyProvider) {

		// Is the constructor annotated with @Autowired/@Inject?
		if (isAnnotatedWithAutowiredOrInject(constructor)) {
			return true;
		}

		AutowireMode autowireMode;

		// Is the test class annotated with @TestConstructor?
		TestConstructor testConstructor = TestContextAnnotationUtils.findMergedAnnotation(testClass, TestConstructor.class);
		if (testConstructor != null) {
			autowireMode = testConstructor.autowireMode();
		}
		else {
			// Custom global default from SpringProperties?
			String value = SpringProperties.getProperty(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME);
			autowireMode = AutowireMode.from(value);

			// Use fallback provider?
			if (autowireMode == null && fallbackPropertyProvider != null) {
				value = fallbackPropertyProvider.get(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME);
				autowireMode = AutowireMode.from(value);
			}
		}

		return (autowireMode == AutowireMode.ALL);
	}

	private static boolean isAnnotatedWithAutowiredOrInject(Constructor<?> constructor) {
		return autowiredAnnotationTypes.stream()
				.anyMatch(annotationType -> AnnotatedElementUtils.hasAnnotation(constructor, annotationType));
	}

}
