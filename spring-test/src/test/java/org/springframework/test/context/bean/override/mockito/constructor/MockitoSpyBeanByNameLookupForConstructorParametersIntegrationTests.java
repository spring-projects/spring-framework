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

package org.springframework.test.context.bean.override.mockito.constructor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.mockito.MockitoAssertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Integration tests for {@link MockitoSpyBean @MockitoSpyBean} that use by-name
 * lookup on constructor parameters.
 *
 * @author Sam Brannen
 * @since 7.1
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/36096">gh-36096</a>
 * @see org.springframework.test.context.bean.override.mockito.MockitoSpyBeanByNameLookupTestMethodScopedExtensionContextIntegrationTests
 */
@SpringJUnitConfig
class MockitoSpyBeanByNameLookupForConstructorParametersIntegrationTests {

	final ExampleService service1;

	final ExampleService service2;

	final ExampleService service3;


	MockitoSpyBeanByNameLookupForConstructorParametersIntegrationTests(
			@MockitoSpyBean ExampleService s1,
			@MockitoSpyBean("s2") ExampleService service2,
			@MockitoSpyBean @Qualifier("s3") ExampleService service3) {

		this.service1 = s1;
		this.service2 = service2;
		this.service3 = service3;
	}


	@Test
	void parameterNameIsUsedAsBeanName(ApplicationContext ctx) {
		assertThat(this.service1)
				.satisfies(MockitoAssertions::assertIsSpy)
				.isSameAs(ctx.getBean("s1"));

		assertThat(this.service1.greeting()).isEqualTo("prod 1");
		verify(this.service1).greeting();
		verifyNoMoreInteractions(this.service1);
	}

	@Test
	void explicitBeanNameOverridesParameterName(ApplicationContext ctx) {
		assertThat(this.service2)
				.satisfies(MockitoAssertions::assertIsSpy)
				.isSameAs(ctx.getBean("s2"));

		assertThat(this.service2.greeting()).isEqualTo("prod 2");
		verify(this.service2).greeting();
		verifyNoMoreInteractions(this.service2);
	}

	@Test
	void qualifierIsUsedToResolveByName(ApplicationContext ctx) {
		assertThat(this.service3)
				.satisfies(MockitoAssertions::assertIsSpy)
				.isSameAs(ctx.getBean("s3"));

		assertThat(this.service3.greeting()).isEqualTo("prod 3");
		verify(this.service3).greeting();
		verifyNoMoreInteractions(this.service3);
	}


	@Nested
	class NestedTests {

		@Autowired
		@Qualifier("s1")
		ExampleService localService1;

		final ExampleService nestedSpy;


		NestedTests(@MockitoSpyBean("s4") ExampleService nestedSpy) {
			this.nestedSpy = nestedSpy;
		}


		@Test
		void spyFromEnclosingClassIsAccessibleViaAutowiring(ApplicationContext ctx) {
			assertThat(this.localService1)
					.satisfies(MockitoAssertions::assertIsSpy)
					.isSameAs(service1)
					.isSameAs(ctx.getBean("s1"));

			assertThat(this.localService1.greeting()).isEqualTo("prod 1");
			verify(this.localService1).greeting();
			verifyNoMoreInteractions(this.localService1);
		}

		@Test
		void nestedConstructorParameterIsASpy(ApplicationContext ctx) {
			assertThat(this.nestedSpy)
					.satisfies(MockitoAssertions::assertIsSpy)
					.isSameAs(ctx.getBean("s4"));

			assertThat(this.nestedSpy.greeting()).isEqualTo("prod 4");
			verify(this.nestedSpy).greeting();
			verifyNoMoreInteractions(this.nestedSpy);
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		ExampleService s1() {
			return new RealExampleService("prod 1");
		}

		@Bean
		ExampleService s2() {
			return new RealExampleService("prod 2");
		}

		@Bean
		ExampleService s3() {
			return new RealExampleService("prod 3");
		}

		@Bean
		ExampleService s4() {
			return new RealExampleService("prod 4");
		}
	}

}
