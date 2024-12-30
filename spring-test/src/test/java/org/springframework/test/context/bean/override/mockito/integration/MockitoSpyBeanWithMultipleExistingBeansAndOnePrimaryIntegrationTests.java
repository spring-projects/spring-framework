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

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.example.ExampleGenericServiceCaller;
import org.springframework.test.context.bean.override.example.IntegerExampleGenericService;
import org.springframework.test.context.bean.override.example.StringExampleGenericService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.mockito.MockitoAssertions.assertIsSpy;
import static org.springframework.test.mockito.MockitoAssertions.assertMockName;

/**
 * Tests that {@link MockitoSpyBean @MockitoSpyBean} can be used to spy on a bean
 * when there are multiple candidates and one is {@link Primary @Primary}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoSpyBeanWithMultipleExistingBeansAndExplicitBeanNameIntegrationTests
 * @see MockitoSpyBeanWithMultipleExistingBeansAndExplicitQualifierIntegrationTests
 */
@ExtendWith(SpringExtension.class)
class MockitoSpyBeanWithMultipleExistingBeansAndOnePrimaryIntegrationTests {

	@MockitoSpyBean
	StringExampleGenericService spy;

	@Autowired
	ExampleGenericServiceCaller caller;


	@Test
	void testSpying() {
		assertIsSpy(spy);
		assertMockName(spy, "two");

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
		@Primary
		StringExampleGenericService two() {
			return new StringExampleGenericService("two");
		}
	}

}
