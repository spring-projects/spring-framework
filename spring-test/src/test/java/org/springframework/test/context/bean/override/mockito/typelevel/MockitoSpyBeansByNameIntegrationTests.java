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

package org.springframework.test.context.bean.override.mockito.typelevel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBeans;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.mockito.MockitoAssertions.assertIsNotMock;
import static org.springframework.test.mockito.MockitoAssertions.assertIsNotSpy;
import static org.springframework.test.mockito.MockitoAssertions.assertIsSpy;

/**
 * Integration tests for {@link MockitoSpyBeans @MockitoSpyBeans} and
 * {@link MockitoSpyBean @MockitoSpyBean} declared "by name" at the class level
 * as a repeatable annotation.
 *
 * @author Sam Brannen
 * @since 6.2.3
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34408">gh-34408</a>
 * @see MockitoSpyBeansByTypeIntegrationTests
 */
@SpringJUnitConfig
@MockitoSpyBean(name = "s1", types = ExampleService.class)
@MockitoSpyBean(name = "s2", types = ExampleService.class)
class MockitoSpyBeansByNameIntegrationTests {

	@Autowired
	ExampleService s1;

	@Autowired
	ExampleService s2;

	@MockitoSpyBean(name = "s3")
	ExampleService service3;

	@Autowired
	@Qualifier("s4")
	ExampleService service4;


	@BeforeEach
	void configureSpies() {
		given(s1.greeting()).willReturn("spy 1");
		given(s2.greeting()).willReturn("spy 2");
		given(service3.greeting()).willReturn("spy 3");
	}

	@Test
	void checkSpiesAndStandardBean() {
		assertIsSpy(s1, "s1");
		assertIsSpy(s2, "s2");
		assertIsSpy(service3, "service3");
		assertIsNotMock(service4, "service4");
		assertIsNotSpy(service4, "service4");

		assertThat(s1.greeting()).isEqualTo("spy 1");
		assertThat(s2.greeting()).isEqualTo("spy 2");
		assertThat(service3.greeting()).isEqualTo("spy 3");
		assertThat(service4.greeting()).isEqualTo("prod 4");
	}


	@Configuration
	static class Config {

		@Bean
		ExampleService s1() {
			return new ExampleService() {
				@Override
				public String greeting() {
					return "prod 1";
				}
			};
		}

		@Bean
		ExampleService s2() {
			return new ExampleService() {
				@Override
				public String greeting() {
					return "prod 2";
				}
			};
		}

		@Bean
		ExampleService s3() {
			return new ExampleService() {
				@Override
				public String greeting() {
					return "prod 3";
				}
			};
		}

		@Bean
		ExampleService s4() {
			return () -> "prod 4";
		}
	}

}
