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

import java.util.HashMap;
import java.util.Map;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MicrometerObservationRegistryTestExecutionListener}.
 *
 * @author Marcin Grzejszczak
 * @author Sam Brannen
 * @since 6.0.10
 */
class MicrometerObservationRegistryTestExecutionListenerTests {

	private final ObservationRegistry originalObservationRegistry = globalObservationRegistry();

	private final TestContext testContext = mock();

	private final StaticApplicationContext applicationContext = new StaticApplicationContext();

	private final Map<String, Object> attributes = new HashMap<>();

	private final TestExecutionListener listener = new MicrometerObservationRegistryTestExecutionListener();


	@BeforeEach
	@SuppressWarnings({ "unchecked", "rawtypes" }) // for raw Class testClass
	void configureTestContextMock() {
		willAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
				.given(testContext).setAttribute(anyString(), any());
		given(testContext.removeAttribute(anyString()))
				.willAnswer(invocation -> attributes.get(invocation.getArgument(0, String.class)));
		given(testContext.getApplicationContext()).willReturn(applicationContext);
		Class testClass = getClass();
		given(testContext.getTestClass()).willReturn(testClass);
	}

	@Test
	void observationRegistryIsNotOverridden() throws Exception {
		assertGlobalObservationRegistryIsSameAsOriginal();

		listener.beforeTestMethod(testContext);
		assertGlobalObservationRegistryIsSameAsOriginal();

		listener.afterTestMethod(testContext);
		assertGlobalObservationRegistryIsSameAsOriginal();
	}

	@Test
	void observationRegistryIsOverriddenByBeanFromApplicationContext() throws Exception {
		assertGlobalObservationRegistryIsSameAsOriginal();

		ObservationRegistry testObservationRegistry = ObservationRegistry.create();
		applicationContext.getDefaultListableBeanFactory().registerSingleton("observationRegistry", testObservationRegistry);

		listener.beforeTestMethod(testContext);
		ObservationRegistry globalObservationRegistry = globalObservationRegistry();
		assertThat(globalObservationRegistry)
				.as("The global ObservationRegistry should have been replaced with the one from the application context")
				.isNotSameAs(originalObservationRegistry)
				.isSameAs(testObservationRegistry);

		listener.afterTestMethod(testContext);
		assertGlobalObservationRegistryIsSameAsOriginal();
	}

	private void assertGlobalObservationRegistryIsSameAsOriginal() {
		assertThat(globalObservationRegistry()).isSameAs(originalObservationRegistry);
	}

	private static ObservationRegistry globalObservationRegistry() {
		return ObservationThreadLocalAccessor.getInstance().getObservationRegistry();
	}

}
