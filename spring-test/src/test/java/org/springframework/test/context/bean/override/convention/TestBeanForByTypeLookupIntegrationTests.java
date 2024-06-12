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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.CustomQualifier;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestBean} that use by-type lookup.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
class TestBeanForByTypeLookupIntegrationTests {

	@TestBean
	ExampleService anyNameForService;

	@TestBean(methodName = "someString")
	@Qualifier("prefer")
	StringBuilder anyNameForStringBuilder;

	@TestBean(methodName = "someString2")
	@CustomQualifier
	StringBuilder anyNameForStringBuilder2;

	static ExampleService anyNameForService() {
		return new RealExampleService("Mocked greeting");
	}

	static StringBuilder someString() {
		return new StringBuilder("Prefer TestBean String");
	}

	static StringBuilder someString2() {
		return new StringBuilder("CustomQualifier TestBean String");
	}


	@Test
	void overrideIsFoundByType(ApplicationContext ctx) {
		assertThat(this.anyNameForService)
				.isSameAs(ctx.getBean("example"))
				.isSameAs(ctx.getBean(ExampleService.class));

		assertThat(this.anyNameForService.greeting()).isEqualTo("Mocked greeting");
	}

	@Test
	void overrideIsFoundByTypeWithQualifierDisambiguation(ApplicationContext ctx) {
		assertThat(this.anyNameForStringBuilder)
				.as("direct qualifier")
				.isSameAs(ctx.getBean("two"))
				.hasToString("Prefer TestBean String");

		assertThat(this.anyNameForStringBuilder2)
				.as("meta qualifier")
				.isSameAs(ctx.getBean("three"))
				.hasToString("CustomQualifier TestBean String");

		assertThat(ctx.getBean("one")).as("no qualifier needed").hasToString("Prod One");
	}


	@Configuration
	static class Config {

		@Bean("example")
		ExampleService bean1() {
			return new RealExampleService("Production hello");
		}

		@Bean("one")
		StringBuilder beanString1() {
			return new StringBuilder("Prod One");
		}

		@Bean("two")
		@Qualifier("prefer")
		StringBuilder beanString2() {
			return new StringBuilder("Prod Two");
		}

		@Bean("three")
		@CustomQualifier
		StringBuilder beanString3() {
			return new StringBuilder("Prod Three");
		}
	}

}
