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

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.bean.override.example.CustomQualifier;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.Mockito.times;
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
public class MockitoSpyBeanForByTypeLookupIntegrationTests {

	@MockitoSpyBean
	ExampleService anyNameForService;

	@MockitoSpyBean
	@Qualifier("prefer")
	StringBuilder ambiguous;

	@MockitoSpyBean
	@CustomQualifier
	StringBuilder ambiguousMeta;


	@Test
	void overrideIsFoundByType(ApplicationContext ctx) {
		assertThat(this.anyNameForService)
				.satisfies(o -> assertThat(Mockito.mockingDetails(o).isSpy()).as("isSpy").isTrue())
				.isSameAs(ctx.getBean("example"))
				.isSameAs(ctx.getBean(ExampleService.class));

		assertThat(this.anyNameForService.greeting()).isEqualTo("Production hello");
		verify(this.anyNameForService, times(1)).greeting();
		verifyNoMoreInteractions(this.anyNameForService);
	}

	@Test
	void overrideIsFoundByTypeAndDisambiguatedByQualifier(ApplicationContext ctx) {
		assertThat(this.ambiguous)
				.satisfies(o -> assertThat(Mockito.mockingDetails(o).isSpy()).as("isSpy").isTrue())
				.isSameAs(ctx.getBean("ambiguous2"));

		assertThatException()
				.isThrownBy(() -> ctx.getBean(StringBuilder.class))
				.withMessageEndingWith("but found 2: ambiguous1,ambiguous2");

		assertThat(this.ambiguous.toString()).isEqualTo("bean3");
		assertThat(this.ambiguous.length()).isEqualTo(5);
		verify(this.ambiguous, times(1)).length();
		verifyNoMoreInteractions(this.ambiguous); //mockito doesn't verify toString
	}

	@Test
	void overrideIsFoundByTypeAndDisambiguatedByMetaQualifier(ApplicationContext ctx) {
		assertThat(this.ambiguousMeta)
				.satisfies(o -> assertThat(Mockito.mockingDetails(o).isSpy()).as("isSpy").isTrue())
				.isSameAs(ctx.getBean("ambiguous1"));

		assertThatException()
				.isThrownBy(() -> ctx.getBean(StringBuilder.class))
				.withMessageEndingWith("but found 2: ambiguous1,ambiguous2");

		assertThat(this.ambiguousMeta.toString()).isEqualTo("bean2");
		assertThat(this.ambiguousMeta.length()).isEqualTo(5);
		verify(this.ambiguousMeta, times(1)).length();
		verifyNoMoreInteractions(this.ambiguousMeta); //mockito doesn't verify toString
	}


	@Configuration
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
	}

}
