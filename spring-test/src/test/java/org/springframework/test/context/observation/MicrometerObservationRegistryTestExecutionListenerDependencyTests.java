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

package org.springframework.test.context.observation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import org.springframework.core.OverridingClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.context.observation.MicrometerObservationRegistryTestExecutionListener.DEPENDENCIES_ERROR_MESSAGE;
import static org.springframework.test.context.observation.MicrometerObservationRegistryTestExecutionListener.OBSERVATION_THREAD_LOCAL_ACCESSOR_CLASS_NAME;
import static org.springframework.test.context.observation.MicrometerObservationRegistryTestExecutionListener.THREAD_LOCAL_ACCESSOR_CLASS_NAME;

/**
 * Tests for {@link MicrometerObservationRegistryTestExecutionListener}
 * behavior regarding required dependencies.
 *
 * @author Sam Brannen
 * @since 6.0.11
 */
class MicrometerObservationRegistryTestExecutionListenerDependencyTests {

	@Test
	void allDependenciesArePresent() throws Exception {
		FilteringClassLoader classLoader = new FilteringClassLoader(getClass().getClassLoader(), name -> false);
		Class<?> listenerClass = classLoader.loadClass(MicrometerObservationRegistryTestExecutionListener.class.getName());
		// Invoke multiple times to ensure consistency.
		IntStream.rangeClosed(1, 5).forEach(n -> assertThatNoException().isThrownBy(() -> instantiateListener(listenerClass)));
	}

	@Test
	void threadLocalAccessorIsNotPresent() throws Exception {
		assertNoClassDefFoundErrorIsThrown(THREAD_LOCAL_ACCESSOR_CLASS_NAME);
	}

	@Test
	void observationThreadLocalAccessorIsNotPresent() throws Exception {
		assertNoClassDefFoundErrorIsThrown(OBSERVATION_THREAD_LOCAL_ACCESSOR_CLASS_NAME);
	}

	private void assertNoClassDefFoundErrorIsThrown(String missingClassName) throws Exception {
		FilteringClassLoader classLoader = new FilteringClassLoader(getClass().getClassLoader(), missingClassName::equals);
		Class<?> listenerClass = classLoader.loadClass(MicrometerObservationRegistryTestExecutionListener.class.getName());
		// Invoke multiple times to ensure the same error message is generated every time.
		IntStream.rangeClosed(1, 5).forEach(n -> assertExceptionThrown(missingClassName, listenerClass));
	}

	private void assertExceptionThrown(String missingClassName, Class<?> listenerClass) {
		assertThatExceptionOfType(InvocationTargetException.class)
				.isThrownBy(() -> instantiateListener(listenerClass))
				.havingCause()
					.isInstanceOf(NoClassDefFoundError.class)
					.withMessage(missingClassName + ". " + DEPENDENCIES_ERROR_MESSAGE);
	}

	private void instantiateListener(Class<?> listenerClass) throws Exception {
		assertThat(listenerClass).isNotNull();
		Constructor<?> constructor = listenerClass.getDeclaredConstructor();
		constructor.setAccessible(true);
		constructor.newInstance();
	}


	static class FilteringClassLoader extends OverridingClassLoader {

		private static final Predicate<? super String> isListenerClass =
				MicrometerObservationRegistryTestExecutionListener.class.getName()::equals;

		private final Predicate<String> classNameFilter;


		FilteringClassLoader(ClassLoader parent, Predicate<String> classNameFilter) {
			super(parent);
			this.classNameFilter = classNameFilter;
		}

		@Override
		protected boolean isEligibleForOverriding(String className) {
			return this.classNameFilter.or(isListenerClass).test(className);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (this.classNameFilter.test(name)) {
				throw new ClassNotFoundException(name);
			}
			return super.loadClass(name, resolve);
		}
	}

}
