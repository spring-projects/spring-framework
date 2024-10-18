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

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.bean.override.example.FailingExampleService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link MockitoBean @MockitoBean} used in combination with scoped-proxy
 * targets.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/5724">gh-5724</a>
 */
@ExtendWith(SpringExtension.class)
public class MockitoBeanAndScopedProxyTests {

	@MockitoBean
	// The ExampleService mock should replace the scoped-proxy FailingExampleService
	// created in the @Configuration class.
	ExampleService service;

	@Autowired
	ExampleServiceCaller serviceCaller;


	@BeforeEach
	void configureServiceMock() {
		given(service.greeting()).willReturn("mock");
	}

	@Test
	void testMocking() {
		assertThat(serviceCaller.sayGreeting()).isEqualTo("I say mock");
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
		ExampleService exampleService() {
			return new FailingExampleService();
		}

		@Bean
		ExampleServiceCaller serviceCaller(ExampleService service) {
			return new ExampleServiceCaller(service);
		}

	}

}
