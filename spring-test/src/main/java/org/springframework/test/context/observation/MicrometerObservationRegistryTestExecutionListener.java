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

import java.lang.reflect.Method;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ReflectionUtils;

/**
 * {@code TestExecutionListener} which provides support for Micrometer's
 * {@link ObservationRegistry}.
 *
 * <p>This listener updates the {@link ObservationThreadLocalAccessor} with the
 * {@code ObservationRegistry} obtained from the test's {@link ApplicationContext},
 * if present.
 *
 * @author Marcin Grzejszczak
 * @author Sam Brannen
 * @since 6.0.10
 */
class MicrometerObservationRegistryTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(MicrometerObservationRegistryTestExecutionListener.class);

	/**
	 * Attribute name for a {@link TestContext} attribute which contains the
	 * {@link ObservationRegistry} that was previously stored in the
	 * {@link ObservationThreadLocalAccessor}.
	 * <p>After each test method, the previously stored {@code ObservationRegistry}
	 * will be restored. If tests run concurrently this might cause issues unless
	 * the {@code ObservationRegistry} is always the same (which should typically
	 * be the case).
	 */
	private static final String PREVIOUS_OBSERVATION_REGISTRY = Conventions.getQualifiedAttributeName(
			MicrometerObservationRegistryTestExecutionListener.class, "previousObservationRegistry");

	static final String DEPENDENCIES_ERROR_MESSAGE = """
			MicrometerObservationRegistryTestExecutionListener requires \
			io.micrometer:micrometer-observation:1.10.8 or higher and \
			io.micrometer:context-propagation:1.0.3 or higher.""";

	static final String THREAD_LOCAL_ACCESSOR_CLASS_NAME = "io.micrometer.context.ThreadLocalAccessor";

	static final String OBSERVATION_THREAD_LOCAL_ACCESSOR_CLASS_NAME =
			"io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor";

	@Nullable
	private static final String ERROR_MESSAGE;

	static {
		// Trigger eager resolution of Micrometer Observation types to ensure that
		// this listener can be properly skipped when SpringFactoriesLoader attempts
		// to load it -- for example, if context-propagation and micrometer-observation
		// are not in the classpath or if the version of ObservationThreadLocalAccessor
		// present does not include the getObservationRegistry() method.

		String errorMessage = null;
		ClassLoader classLoader = MicrometerObservationRegistryTestExecutionListener.class.getClassLoader();
		String classToCheck = THREAD_LOCAL_ACCESSOR_CLASS_NAME;
		try {
			Class.forName(classToCheck, false, classLoader);
			classToCheck = OBSERVATION_THREAD_LOCAL_ACCESSOR_CLASS_NAME;
			Class<?> clazz = Class.forName(classToCheck, false, classLoader);
			Method method = ReflectionUtils.findMethod(clazz, "getObservationRegistry");
			if (method == null) {
				errorMessage = classToCheck + ". Method getObservationRegistry() not found. " + DEPENDENCIES_ERROR_MESSAGE;
			}
		}
		catch (Throwable ex) {
			errorMessage = classToCheck + ". " + DEPENDENCIES_ERROR_MESSAGE;
		}
		ERROR_MESSAGE = errorMessage;
	}


	public MicrometerObservationRegistryTestExecutionListener() {
		// If required dependencies are missing, throw a NoClassDefFoundError so
		// that this listener will be properly skipped in TestContextFailureHandler.
		if (ERROR_MESSAGE != null) {
			throw new NoClassDefFoundError(ERROR_MESSAGE);
		}
	}


	/**
	 * Returns {@code 2500}.
	 */
	@Override
	public final int getOrder() {
		return 2500;
	}

	/**
	 * If the test's {@link ApplicationContext} contains an {@link ObservationRegistry}
	 * bean, this method retrieves the {@code ObservationRegistry} currently stored
	 * in {@link ObservationThreadLocalAccessor}, saves a reference to the original
	 * registry as a {@link TestContext} attribute (to be restored in
	 * {@link #afterTestMethod(TestContext)}), and sets the registry from the test's
	 * {@code ApplicationContext} in {@link ObservationThreadLocalAccessor}.
	 * @param testContext the test context for the test; never {@code null}
	 * @see #afterTestMethod(TestContext)
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) {
		testContext.getApplicationContext().getBeanProvider(ObservationRegistry.class)
				.ifAvailable(registry -> {
					if (logger.isDebugEnabled()) {
						logger.debug("""
								Registering ObservationRegistry from ApplicationContext in \
								ObservationThreadLocalAccessor for test class \
								""" + testContext.getTestClass().getName());
					}
					ObservationThreadLocalAccessor accessor = ObservationThreadLocalAccessor.getInstance();
					testContext.setAttribute(PREVIOUS_OBSERVATION_REGISTRY, accessor.getObservationRegistry());
					accessor.setObservationRegistry(registry);
				});
	}

	/**
	 * Retrieves the original {@link ObservationRegistry} that was saved in
	 * {@link #beforeTestMethod(TestContext)} and sets it in
	 * {@link ObservationThreadLocalAccessor}.
	 * @param testContext the test context for the test; never {@code null}
	 * @see #beforeTestMethod(TestContext)
	 */
	@Override
	public void afterTestMethod(TestContext testContext) {
		ObservationRegistry previousObservationRegistry =
				(ObservationRegistry) testContext.removeAttribute(PREVIOUS_OBSERVATION_REGISTRY);
		if (previousObservationRegistry != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Restoring ObservationRegistry in ObservationThreadLocalAccessor for test class " +
						testContext.getTestClass().getName());
			}
			ObservationThreadLocalAccessor.getInstance().setObservationRegistry(previousObservationRegistry);
		}
	}

}
