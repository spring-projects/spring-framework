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

package org.springframework.test.context.bean.override.convention;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Fallback;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link TestBean @TestBean} can be used to override a bean by-type
 * when there are multiple candidates and only one that is not a fallback.
 *
 * @author Sam Brannen
 * @since 6.2.1
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext
class TestBeanWithMultipleExistingBeansAndOneNonFallbackIntegrationTests {

	@TestBean
	ExampleService service;

	@Autowired
	List<ExampleService> services;


	static ExampleService service() {
		return () -> "overridden";
	}


	@Test
	void test() {
		assertThat(service.greeting()).isEqualTo("overridden");
		assertThat(services).extracting(ExampleService::greeting)
				.containsExactlyInAnyOrder("overridden", "two", "three");
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		ExampleService one() {
			return () -> "one";
		}

		@Bean
		@Fallback
		ExampleService two() {
			return () -> "two";
		}

		@Bean
		@Fallback
		ExampleService three() {
			return () -> "three";
		}
	}

}
