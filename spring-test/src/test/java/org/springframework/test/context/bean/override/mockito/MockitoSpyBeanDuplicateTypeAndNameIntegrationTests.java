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

package org.springframework.test.context.bean.override.mockito;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.mockito.MockitoAssertions.assertIsNotSpy;
import static org.springframework.test.mockito.MockitoAssertions.assertIsSpy;

/**
 * Integration tests for duplicate {@link MockitoSpyBean @MockitoSpyBean}
 * declarations for the same target bean, selected by-name.
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see MockitoBeanDuplicateTypeIntegrationTests
 * @see MockitoSpyBeanDuplicateTypeIntegrationTests
 */
@SpringJUnitConfig
public class MockitoSpyBeanDuplicateTypeAndNameIntegrationTests {

	@MockitoSpyBean("exampleService1")
	ExampleService service1;

	@MockitoSpyBean("exampleService1")
	ExampleService service2;

	@Autowired
	ExampleService exampleService2;

	@Autowired
	List<ExampleService> services;


	@Test
	void duplicateMocksShouldHaveBeenCreated() {
		assertThat(service1).isSameAs(service2);
		assertThat(services).containsExactly(service1, exampleService2);

		assertIsSpy(service1, "service1");
		assertIsNotSpy(exampleService2, "exampleService2");
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		ExampleService exampleService1() {
			return new RealExampleService("@Bean 1");
		}

		@Bean
		ExampleService exampleService2() {
			return new RealExampleService("@Bean 2");
		}
	}

}
