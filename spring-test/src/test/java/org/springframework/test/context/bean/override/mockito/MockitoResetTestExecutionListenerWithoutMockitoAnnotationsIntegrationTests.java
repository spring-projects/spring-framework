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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link MockitoResetTestExecutionListener} without a
 * {@link MockitoBean @MockitoBean} or {@link MockitoSpyBean @MockitoSpyBean} field.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoResetTestExecutionListenerWithMockitoBeanIntegrationTests
 */
@SpringJUnitConfig
@TestMethodOrder(MethodOrderer.MethodName.class)
class MockitoResetTestExecutionListenerWithoutMockitoAnnotationsIntegrationTests {

	@Autowired
	ApplicationContext context;


	@Test
	void test001() {
		ExampleService nonSingletonFactoryBean = getMock("nonSingletonFactoryBean");

		given(getMock("none").greeting()).willReturn("none");
		given(getMock("before").greeting()).willReturn("before");
		given(getMock("after").greeting()).willReturn("after");
		given(getMock("singletonFactoryBean").greeting()).willReturn("singletonFactoryBean");
		given(nonSingletonFactoryBean.greeting()).willReturn("nonSingletonFactoryBean");

		assertThat(getMock("none").greeting()).isEqualTo("none");
		assertThat(getMock("before").greeting()).isEqualTo("before");
		assertThat(getMock("after").greeting()).isEqualTo("after");
		assertThat(getMock("singletonFactoryBean").greeting()).isEqualTo("singletonFactoryBean");

		// The saved reference should have been mocked.
		assertThat(nonSingletonFactoryBean.greeting()).isEqualTo("nonSingletonFactoryBean");
		// A new reference should have not been mocked.
		assertThat(getMock("nonSingletonFactoryBean").greeting()).isNull();

		// getMock("nonSingletonFactoryBean") has been invoked twice in this method.
		assertThat(context.getBean(NonSingletonFactoryBean.class).getObjectInvocations).isEqualTo(2);
	}

	@Disabled("MockReset is currently only honored if @MockitoBean or @MockitoSpyBean is used")
	@Test
	void test002() {
		// Should not have been reset.
		assertThat(getMock("none").greeting()).isEqualTo("none");

		// Should have been reset.
		assertThat(getMock("before").greeting()).isNull();
		assertThat(getMock("after").greeting()).isNull();
		assertThat(getMock("singletonFactoryBean").greeting()).isNull();

		// A non-singleton FactoryBean always creates a new mock instance. Thus,
		// resetting is irrelevant, and the greeting should be null.
		assertThat(getMock("nonSingletonFactoryBean").greeting()).isNull();

		// getMock("nonSingletonFactoryBean") has been invoked twice in test001()
		// and once in this method.
		assertThat(context.getBean(NonSingletonFactoryBean.class).getObjectInvocations).isEqualTo(3);
	}

	private ExampleService getMock(String name) {
		return context.getBean(name, ExampleService.class);
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		ExampleService none() {
			return mock(ExampleService.class);
		}

		@Bean
		ExampleService before() {
			return mock(ExampleService.class, MockReset.before());
		}

		@Bean
		ExampleService after() {
			return mock(ExampleService.class, MockReset.after());
		}

		@Bean
		@Lazy
		ExampleService fail() {
			// Spring Boot gh-5870
			throw new RuntimeException();
		}

		@Bean
		BrokenFactoryBean brokenFactoryBean() {
			// Spring Boot gh-7270
			return new BrokenFactoryBean();
		}

		@Bean
		WorkingFactoryBean singletonFactoryBean() {
			return new WorkingFactoryBean();
		}

		@Bean
		NonSingletonFactoryBean nonSingletonFactoryBean() {
			return new NonSingletonFactoryBean();
		}

	}

	static class BrokenFactoryBean implements FactoryBean<String> {

		@Override
		public String getObject() {
			throw new IllegalStateException();
		}

		@Override
		public Class<?> getObjectType() {
			return String.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

	static class WorkingFactoryBean implements FactoryBean<ExampleService> {

		private final ExampleService service = mock(ExampleService.class, MockReset.before());

		@Override
		public ExampleService getObject() {
			return this.service;
		}

		@Override
		public Class<?> getObjectType() {
			return ExampleService.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

	static class NonSingletonFactoryBean implements FactoryBean<ExampleService> {

		private int getObjectInvocations = 0;

		@Override
		public ExampleService getObject() {
			this.getObjectInvocations++;
			return mock(ExampleService.class, MockReset.before());
		}

		@Override
		public Class<?> getObjectType() {
			return ExampleService.class;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}

	}

}
