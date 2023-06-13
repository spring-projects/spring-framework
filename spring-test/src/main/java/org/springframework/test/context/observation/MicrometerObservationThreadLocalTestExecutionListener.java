/*
 * Copyright 2002-2023 the original author or authors.
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

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Conventions;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * {@code ObservationThreadLocalTestExecutionListener} is an implementation of the {@link TestExecutionListener}
 * SPI that updates the {@link ObservationThreadLocalAccessor} with the {@link ObservationRegistry}
 * taken from the {@link ApplicationContext} present in the {@link TestContext}.
 *
 * <p>This implementation is not thread-safe.
 *
 * @author Marcin Grzejszczak
 * @since 6.1
 */
public class MicrometerObservationThreadLocalTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Attribute name for a {@link TestContext} attribute which contains the previously
	 * set {@link ObservationRegistry} on the {@link ObservationThreadLocalAccessor}.
	 * <p>After all tests from the current test class have completed, the previously stored {@link ObservationRegistry}
	 * will be restored. If tests are ran concurrently this might cause issues
	 * unless the {@link ObservationRegistry} is always the same (which should be the case most frequently).
	 */
	private static final String PREVIOUS_OBSERVATION_REGISTRY = Conventions.getQualifiedAttributeName(
			MicrometerObservationThreadLocalTestExecutionListener.class, "previousObservationRegistry");

	/**
	 * Retrieves the current {@link ObservationRegistry} stored
	 * on {@link ObservationThreadLocalAccessor} instance and stores it
	 * in the {@link TestContext} attributes and overrides it with
	 * one stored in {@link ApplicationContext} associated with
	 * the {@link TestContext}.
	 * @param testContext the test context for the test; never {@code null}
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) {
		testContext.setAttribute(PREVIOUS_OBSERVATION_REGISTRY,
				ObservationThreadLocalAccessor.getInstance().getObservationRegistry());
		testContext.getApplicationContext()
				.getBeanProvider(ObservationRegistry.class)
				.ifAvailable(observationRegistry ->
						ObservationThreadLocalAccessor.getInstance()
								.setObservationRegistry(observationRegistry));
	}

	/**
	 * Retrieves the previously stored {@link ObservationRegistry} and sets it back
	 * on the {@link ObservationThreadLocalAccessor} instance.
	 * @param testContext the test context for the test; never {@code null}
	 */
	@Override
	public void afterTestMethod(TestContext testContext) {
		ObservationRegistry previousObservationRegistry =
				(ObservationRegistry) testContext.getAttribute(PREVIOUS_OBSERVATION_REGISTRY);
		if (previousObservationRegistry != null) {
			ObservationThreadLocalAccessor.getInstance()
					.setObservationRegistry(previousObservationRegistry);
		}
	}


	/**
	 * Returns {@code 3500}.
	 */
	@Override
	public final int getOrder() {
		return 3500;
	}
}
