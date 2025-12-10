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

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.bean.override.example.CustomQualifier;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.mockito.MockitoAssertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Integration tests for {@link MockitoSpyBean} that use by-type lookup.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
public class MockitoSpyBeanByTypeLookupIntegrationTests {

	@MockitoSpyBean
	ExampleService anyNameForService;

	@MockitoSpyBean
	@Qualifier("prefer")
	StringHolder ambiguous;

	@MockitoSpyBean
	@CustomQualifier
	StringHolder ambiguousMeta;

	@MockitoSpyBean
	AnotherService prototypeService;


	@Test
	void overrideIsFoundByType(ApplicationContext ctx) {
		assertThat(this.anyNameForService)
				.satisfies(MockitoAssertions::assertIsSpy)
				.isSameAs(ctx.getBean("example"))
				.isSameAs(ctx.getBean(ExampleService.class));

		assertThat(this.anyNameForService.greeting()).isEqualTo("Production hello");
		verify(this.anyNameForService).greeting();
		verifyNoMoreInteractions(this.anyNameForService);
	}

	@Test
	void overrideIsFoundByTypeAndDisambiguatedByQualifier(ApplicationContext ctx) {
		assertThat(this.ambiguous)
				.satisfies(MockitoAssertions::assertIsSpy)
				.isSameAs(ctx.getBean("ambiguous2"));

		assertThatException()
				.isThrownBy(() -> ctx.getBean(StringHolder.class))
				.withMessageEndingWith("but found 2: ambiguous1,ambiguous2");

		assertThat(this.ambiguous.getValue()).isEqualTo("bean3");
		assertThat(this.ambiguous.size()).isEqualTo(5);
		verify(this.ambiguous).getValue();
		verify(this.ambiguous).size();
		verifyNoMoreInteractions(this.ambiguous);
	}

	@Test
	void overrideIsFoundByTypeAndDisambiguatedByMetaQualifier(ApplicationContext ctx) {
		assertThat(this.ambiguousMeta)
				.satisfies(MockitoAssertions::assertIsSpy)
				.isSameAs(ctx.getBean("ambiguous1"));

		assertThatException()
				.isThrownBy(() -> ctx.getBean(StringHolder.class))
				.withMessageEndingWith("but found 2: ambiguous1,ambiguous2");

		assertThat(this.ambiguousMeta.getValue()).isEqualTo("bean2");
		assertThat(this.ambiguousMeta.size()).isEqualTo(5);
		verify(this.ambiguousMeta).getValue();
		verify(this.ambiguousMeta).size();
		verifyNoMoreInteractions(this.ambiguousMeta);
	}

	@Test
	void overrideIsFoundByTypeForPrototype(ConfigurableApplicationContext ctx) {
		assertThat(this.prototypeService)
				.satisfies(MockitoAssertions::assertIsSpy)
				.isSameAs(ctx.getBean("anotherService"))
				.isSameAs(ctx.getBean(AnotherService.class));
		assertThat(ctx.getBeanFactory().getBeanDefinition("anotherService").isSingleton()).as("isSingleton").isTrue();

		assertThat(this.prototypeService.hello()).isEqualTo("Production Hello");
		verify(this.prototypeService).hello();
		verifyNoMoreInteractions(this.prototypeService);
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean("example")
		ExampleService bean1() {
			return new RealExampleService("Production hello");
		}

		@Bean("ambiguous1")
		@Order(1)
		@CustomQualifier
		StringHolder bean2() {
			return new StringHolder("bean2");
		}

		@Bean("ambiguous2")
		@Order(2)
		@Qualifier("prefer")
		StringHolder bean3() {
			return new StringHolder("bean3");
		}

		@Bean("anotherService")
		@Scope("prototype")
		AnotherService bean4() {
			return new DefaultAnotherService("Production Hello");
		}
	}

	static class StringHolder {

		private final String value;

		StringHolder(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		public int size() {
			return this.value.length();
		}
	}

	public interface AnotherService {

		String hello();
	}

	record DefaultAnotherService(String message) implements AnotherService {

		@Override
		public String hello() {
			return this.message;
		}
	}

}
