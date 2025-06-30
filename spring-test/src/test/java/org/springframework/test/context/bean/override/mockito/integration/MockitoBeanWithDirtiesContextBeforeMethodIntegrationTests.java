/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.RepeatedTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;

/**
 * Integration tests for using {@link MockitoBean @MockitoBean} with
 * {@link DirtiesContext @DirtiesContext} and {@link MethodMode#BEFORE_METHOD}.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoSpyBeanWithDirtiesContextBeforeMethodIntegrationTests
 */
@SpringJUnitConfig
class MockitoBeanWithDirtiesContextBeforeMethodIntegrationTests {

	@Autowired
	ExampleServiceCaller caller;

	@MockitoBean
	ExampleService service;

	@Autowired
	ExampleService autowiredService;


	@RepeatedTest(2)
	@DirtiesContext(methodMode = BEFORE_METHOD)
	void testMocking() {
		assertThat(service).isSameAs(autowiredService);

		given(service.greeting()).willReturn("Spring");
		assertThat(caller.sayGreeting()).isEqualTo("I say Spring");
	}


	@Configuration(proxyBeanMethods = false)
	@Import(ExampleServiceCaller.class)
	static class Config {
	}

}
