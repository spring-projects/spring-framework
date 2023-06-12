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

import java.util.HashMap;
import java.util.Map;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.context.TestContext;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

class MicrometerObservationThreadLocalTestExecutionListenerTests {

	ObservationRegistry originalObservationRegistry = ObservationThreadLocalAccessor.getInstance().getObservationRegistry();

	TestContext testContext = mock();

	StaticApplicationContext applicationContext = new StaticApplicationContext();

	Map<String, Object> attributes = new HashMap<>();

	MicrometerObservationThreadLocalTestExecutionListener listener = new MicrometerObservationThreadLocalTestExecutionListener();

	@BeforeEach
	void setup() {
		willAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1))).given(testContext).setAttribute(anyString(), any());
		given(testContext.getAttribute(anyString())).willAnswer(invocation -> attributes.get(invocation.getArgument(0, String.class)));
		given(testContext.getApplicationContext()).willReturn(applicationContext);
	}

	@Test
	void observationRegistryShouldNotBeOverridden() throws Exception {
		listener.beforeTestMethod(testContext);
		thenObservationRegistryOnOTLAIsSameAsOriginal();
		listener.afterTestMethod(testContext);
		thenObservationRegistryOnOTLAIsSameAsOriginal();
	}

	@Test
	void observationRegistryOverriddenByBeanFromTestContext() throws Exception {
		ObservationRegistry newObservationRegistry = ObservationRegistry.create();
		applicationContext.getDefaultListableBeanFactory().registerSingleton("observationRegistry", newObservationRegistry);

		listener.beforeTestMethod(testContext);
		ObservationRegistry otlaObservationRegistry = ObservationThreadLocalAccessor.getInstance().getObservationRegistry();
		then(otlaObservationRegistry)
				.as("During the test we want the original ObservationRegistry to be replaced with the one present in this application context")
				.isNotSameAs(originalObservationRegistry)
				.isSameAs(newObservationRegistry);

		listener.afterTestMethod(testContext);
		thenObservationRegistryOnOTLAIsSameAsOriginal();
	}

	private void thenObservationRegistryOnOTLAIsSameAsOriginal() {
		then(ObservationThreadLocalAccessor.getInstance().getObservationRegistry()).isSameAs(originalObservationRegistry);
	}

}
