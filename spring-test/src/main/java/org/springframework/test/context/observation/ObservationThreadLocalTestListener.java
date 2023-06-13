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

/**
 * {@code ObservationThreadLocalTestExecutionListener} is an implementation of the {@link TestExecutionListener}
 * SPI that updates the {@link ObservationThreadLocalAccessor} with the {@link ObservationRegistry}
 * taken from the {@link ApplicationContext} present in the {@link TestContext}.
 *
 * This implementation is not thread-safe.
 *
 * @author Marcin Grzejszczak
 * @since 6.0
 */
public class ObservationThreadLocalTestListener implements TestExecutionListener {

	/**
	 * Attribute name for a {@link TestContext} attribute which contains the previously
	 * set {@link ObservationRegistry} on the {@link ObservationThreadLocalAccessor}.
	 * After all tests from the given test class get ran, the previously stored {@link ObservationRegistry}
	 * will be restored. If tests are ran concurrently this might cause issues
	 * unless the {@link ObservationRegistry} is always the same (which should be the case most frequently).
	 */
	public static final String PREVIOUS_OBSERVATION_REGISTRY = Conventions.getQualifiedAttributeName(
			ObservationThreadLocalTestListener.class, "previousObservationRegistry");

	@Override
	public void beforeTestClass(TestContext testContext) throws Exception {
		testContext.setAttribute(PREVIOUS_OBSERVATION_REGISTRY, ObservationThreadLocalAccessor.getInstance().getObservationRegistry());
		testContext.getApplicationContext().getBeanProvider(ObservationRegistry.class)
				.ifAvailable(observationRegistry -> ObservationThreadLocalAccessor.getInstance().setObservationRegistry(observationRegistry));
	}

	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		ObservationRegistry previousObservationRegistry = (ObservationRegistry) testContext.getAttribute(PREVIOUS_OBSERVATION_REGISTRY);
		if (previousObservationRegistry != null) {
			ObservationThreadLocalAccessor.getInstance().setObservationRegistry(previousObservationRegistry);
		}
	}
}
