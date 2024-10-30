/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;
import org.mockito.MockingDetails;
import org.mockito.mock.MockCreationSettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.example.ExampleGenericServiceCaller;
import org.springframework.test.context.bean.override.example.IntegerExampleGenericService;
import org.springframework.test.context.bean.override.example.StringExampleGenericService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockingDetails;

/**
 * Tests that {@link MockitoSpyBean @MockitoSpyBean} can be used to spy on a bean
 * when there are multiple candidates and an explicit bean name is supplied to
 * select one of the candidates.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 * @see MockitoSpyBeanWithMultipleExistingBeansAndExplicitQualifierIntegrationTests
 * @see MockitoSpyBeanWithMultipleExistingBeansAndOnePrimaryIntegrationTests
 */
@SpringJUnitConfig
class MockitoSpyBeanWithMultipleExistingBeansAndExplicitBeanNameIntegrationTests {

	@MockitoSpyBean("stringService")
	StringExampleGenericService spy;

	@Autowired
	ExampleGenericServiceCaller caller;


	@Test
	void test() {
		MockingDetails mockingDetails = mockingDetails(spy);
		MockCreationSettings<?> mockSettings = mockingDetails.getMockCreationSettings();
		assertThat(mockingDetails.isSpy()).as("is spy").isTrue();
		assertThat(mockSettings.getMockName()).hasToString("stringService");

		assertThat(caller.sayGreeting()).isEqualTo("I say two 123");
		then(spy).should().greeting();
	}


	@Configuration(proxyBeanMethods = false)
	@Import({ ExampleGenericServiceCaller.class, IntegerExampleGenericService.class })
	static class Config {

		@Bean
		StringExampleGenericService one() {
			return new StringExampleGenericService("one");
		}

		@Bean
		// "stringService" matches the constructor argument name in ExampleGenericServiceCaller
		StringExampleGenericService stringService() {
			return new StringExampleGenericService("two");
		}
	}

}
