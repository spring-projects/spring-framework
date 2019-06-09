/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.TestConstructor;

/**
 * Utility methods for working with {@link TestConstructor @TestConstructor}.
 *
 * <p>Primarily intended for use within the framework.
 *
 * @author Sam Brannen
 * @since 5.2
 * @see TestConstructor
 */
public abstract class TestConstructorUtils {

	private TestConstructorUtils() {
	}


	/**
	 * Determine if the supplied executable for the given test class is an
	 * autowirable constructor.
	 *
	 * <p>This method delegates to {@link #isAutowirableConstructor(Constructor, Class)}
	 * if the executable is a constructor.
	 *
	 * @param executable an executable for the test class
	 * @param testClass the test class
	 * @return {@code true} if the executable is an autowirable constructor
	 * @see #isAutowirableConstructor(Constructor, Class)
	 */
	public static boolean isAutowirableConstructor(Executable executable, Class<?> testClass) {
		return (executable instanceof Constructor &&
				isAutowirableConstructor((Constructor<?>) executable, testClass));
	}

	/**
	 * Determine if the supplied constructor for the given test class is
	 * autowirable.
	 *
	 * <p>A constructor is considered to be autowirable if one of the following
	 * conditions is {@code true}.
	 *
	 * <ol>
	 * <li>The constructor is annotated with {@link Autowired @Autowired}.</li>
	 * <li>{@link TestConstructor @TestConstructor} is <em>present</em> or
	 * <em>meta-present</em> on the test class with
	 * {@link TestConstructor#autowire autowire} set to {@code true}.</li>
	 * <li>The default <em>test constructor autowire</em> mode is set to {@code true}
	 * (see {@link TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_PROPERTY_NAME}).</li>
	 * </ol>
	 *
	 * @param constructor a constructor for the test class
	 * @param testClass the test class
	 * @return {@code true} if the constructor is autowirable
	 * @see #isAutowirableConstructor(Executable, Class)
	 */
	public static boolean isAutowirableConstructor(Constructor<?> constructor, Class<?> testClass) {
		// Is the constructor annotated with @Autowired?
		if (AnnotatedElementUtils.hasAnnotation(constructor, Autowired.class)) {
			return true;
		}
		// Is the test class annotated with @TestConstructor?
		TestConstructor testConstructor = AnnotatedElementUtils.findMergedAnnotation(testClass, TestConstructor.class);
		if (testConstructor != null) {
			return testConstructor.autowire();
		}
		// Else use global default.
		return SpringProperties.getFlag(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_PROPERTY_NAME);
	}

}
