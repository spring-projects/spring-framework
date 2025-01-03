/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito.mockbeans;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for {@link MockitoBeans @MockitoBeans} and
 * {@link MockitoBean @MockitoBean} declared "by name" at the class level as a
 * repeatable annotation.
 *
 * @author Sam Brannen
 * @since 6.2.2
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/33925">gh-33925</a>
 * @see MockitoBeansByTypeIntegrationTests
 */
@SpringJUnitConfig
@MockitoBean(name = "s1", types = ExampleService.class)
@MockitoBean(name = "s2", types = ExampleService.class)
class MockitoBeansByNameIntegrationTests {

	@Autowired
	ExampleService s1;

	@Autowired
	ExampleService s2;

	@MockitoBean(name = "s3")
	ExampleService service3;

	@Autowired
	@Qualifier("s4")
	ExampleService service4;


	@BeforeEach
	void configureMocks() {
		given(s1.greeting()).willReturn("mock 1");
		given(s2.greeting()).willReturn("mock 2");
		given(service3.greeting()).willReturn("mock 3");
	}

	@Test
	void checkMocksAndStandardBean() {
		assertThat(s1.greeting()).isEqualTo("mock 1");
		assertThat(s2.greeting()).isEqualTo("mock 2");
		assertThat(service3.greeting()).isEqualTo("mock 3");
		assertThat(service4.greeting()).isEqualTo("prod 4");
	}

	@Configuration
	static class Config {

		@Bean
		ExampleService s1() {
			return () -> "prod 1";
		}

		@Bean
		ExampleService s2() {
			return () -> "prod 2";
		}

		@Bean
		ExampleService s3() {
			return () -> "prod 3";
		}

		@Bean
		ExampleService s4() {
			return () -> "prod 4";
		}
	}

}
