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

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.mockito.MockitoAssertions.assertIsMock;

/**
 * Integration tests for {@link MockitoBean} that use by-type lookup.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
public class MockitoBeanByTypeLookupIntegrationTests {

	@MockitoBean
	AnotherService serviceIsNotABean;

	@MockitoBean
	ExampleService anyNameForService;

	@MockitoBean
	@Qualifier("prefer")
	StringBuilder ambiguous;

	@MockitoBean
	@CustomQualifier
	StringBuilder ambiguousMeta;

	@MockitoBean
	YetAnotherService yetAnotherService;


	@Test
	void mockIsCreatedWhenNoCandidateIsFound() {
		assertIsMock(this.serviceIsNotABean);

		when(this.serviceIsNotABean.hello()).thenReturn("Mocked hello");

		assertThat(this.serviceIsNotABean.hello()).isEqualTo("Mocked hello");
		verify(this.serviceIsNotABean, times(1)).hello();
		verifyNoMoreInteractions(this.serviceIsNotABean);
	}

	@Test
	void overrideIsFoundByType(ApplicationContext ctx) {
		assertThat(this.anyNameForService)
				.satisfies(MockitoAssertions::assertIsMock)
				.isSameAs(ctx.getBean("example"))
				.isSameAs(ctx.getBean(ExampleService.class));

		when(this.anyNameForService.greeting()).thenReturn("Mocked greeting");

		assertThat(this.anyNameForService.greeting()).isEqualTo("Mocked greeting");
		verify(this.anyNameForService, times(1)).greeting();
		verifyNoMoreInteractions(this.anyNameForService);
	}

	@Test
	void overrideIsFoundByTypeAndDisambiguatedByQualifier(ApplicationContext ctx) {
		assertThat(this.ambiguous)
				.satisfies(MockitoAssertions::assertIsMock)
				.isSameAs(ctx.getBean("ambiguous2"));

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
				.isThrownBy(() -> ctx.getBean(StringBuilder.class))
				.satisfies(ex -> assertThat(ex.getBeanNamesFound()).containsOnly("ambiguous1", "ambiguous2"));

		assertThat(this.ambiguous).isEmpty();
		assertThat(this.ambiguous.substring(0)).isNull();
		verify(this.ambiguous, times(1)).length();
		verify(this.ambiguous, times(1)).substring(anyInt());
		verifyNoMoreInteractions(this.ambiguous);
	}

	@Test
	void overrideIsFoundByTypeAndDisambiguatedByMetaQualifier(ApplicationContext ctx) {
		assertThat(this.ambiguousMeta)
				.satisfies(MockitoAssertions::assertIsMock)
				.isSameAs(ctx.getBean("ambiguous1"));

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
				.isThrownBy(() -> ctx.getBean(StringBuilder.class))
				.satisfies(ex -> assertThat(ex.getBeanNamesFound()).containsOnly("ambiguous1", "ambiguous2"));

		assertThat(this.ambiguousMeta).isEmpty();
		assertThat(this.ambiguousMeta.substring(0)).isNull();
		verify(this.ambiguousMeta, times(1)).length();
		verify(this.ambiguousMeta, times(1)).substring(anyInt());
		verifyNoMoreInteractions(this.ambiguousMeta);
	}

	@Test
	void overrideIsFoundByTypeForPrototype(ConfigurableApplicationContext ctx) {
		assertThat(this.yetAnotherService)
				.satisfies(MockitoAssertions::assertIsMock)
				.isSameAs(ctx.getBean("YAS"))
				.isSameAs(ctx.getBean(YetAnotherService.class));
		assertThat(ctx.getBeanFactory().getBeanDefinition("YAS").isSingleton()).as("isSingleton").isTrue();

		when(this.yetAnotherService.hello()).thenReturn("Mocked greeting");

		assertThat(this.yetAnotherService.hello()).isEqualTo("Mocked greeting");
		verify(this.yetAnotherService, times(1)).hello();
		verifyNoMoreInteractions(this.yetAnotherService);
	}


	public interface AnotherService {

		String hello();
	}

	public interface YetAnotherService {

		String hello();
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
		StringBuilder bean2() {
			return new StringBuilder("bean2");
		}

		@Bean("ambiguous2")
		@Order(2)
		@Qualifier("prefer")
		StringBuilder bean3() {
			return new StringBuilder("bean3");
		}

		@Bean("YAS")
		@Scope("prototype")
		YetAnotherService bean4() {
			return () -> "Production Hello";
		}
	}

}
