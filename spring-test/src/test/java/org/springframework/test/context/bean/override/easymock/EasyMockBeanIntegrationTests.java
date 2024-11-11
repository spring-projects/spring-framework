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

package org.springframework.test.context.bean.override.easymock;

import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

/**
 * Integration tests for {@link EasyMockBean @EasyMockBean}.
 *
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
@TestExecutionListeners(listeners = EasyMockResetTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@TestMethodOrder(OrderAnnotation.class)
public class EasyMockBeanIntegrationTests {

	@Autowired
	ApplicationContext ctx;

	@EasyMockBean
	ExampleService service;

	@Test
	@Order(1)
	void test1() {
		assertThat(ctx.getBean("service", ExampleService.class))
				.satisfies(this::assertIsEasyMock)
				.isSameAs(service);

		// Before mock setup
		assertThat(service.greeting()).isNull();
		reset(service);

		// After mock setup
		expect(service.greeting()).andReturn("mocked");
		replay(service);
		assertThat(service.greeting()).isEqualTo("mocked");
	}

	@Test
	@Order(2)
	void test2() {
		assertThat(ctx.getBean("service", ExampleService.class))
				.satisfies(this::assertIsEasyMock)
				.isSameAs(service);

		// Before mock setup
		assertThat(service.greeting()).isNull();
		reset(service);

		// After mock setup
		expect(service.greeting()).andReturn("mocked");
		replay(service);
		assertThat(service.greeting()).isEqualTo("mocked");
	}


	private void assertIsEasyMock(Object obj) {
		assertThat(EasyMockSupport.isAMock(obj)).as("is EasyMock mock").isTrue();
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		ExampleService service() {
			return () -> "enigma";
		}
	}

}
