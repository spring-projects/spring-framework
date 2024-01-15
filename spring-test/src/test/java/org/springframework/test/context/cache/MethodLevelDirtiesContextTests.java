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

package org.springframework.test.context.cache;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;

/**
 * Integration test which verifies correct interaction between the
 * {@link DirtiesContextBeforeModesTestExecutionListener},
 * {@link DependencyInjectionTestExecutionListener}, and
 * {@link DirtiesContextTestExecutionListener} when
 * {@link DirtiesContext @DirtiesContext} is used at the method level.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@SpringJUnitConfig
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MethodLevelDirtiesContextTests {

	private static final AtomicInteger contextCount = new AtomicInteger();


	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private Integer count;


	@Test
	@Order(1)
	void basics() {
		performAssertions(1);
	}

	@Test
	@Order(2)
	@DirtiesContext(methodMode = BEFORE_METHOD)
	void dirtyContextBeforeTestMethod() {
		performAssertions(2);
	}

	@Test
	@Order(3)
	@DirtiesContext
	void dirtyContextAfterTestMethod() {
		performAssertions(2);
	}

	@Test
	@Order(4)
	void backToBasics() {
		performAssertions(3);
	}

	private void performAssertions(int expectedContextCreationCount) {
		assertThat(this.context).as("context must not be null").isNotNull();
		assertThat(this.context.isActive()).as("context must be active").isTrue();

		assertThat(this.count).as("count must not be null").isNotNull();
		assertThat(this.count).as("count").isEqualTo(expectedContextCreationCount);

		assertThat(contextCount.get()).as("context creation count: ").isEqualTo(expectedContextCreationCount);
	}


	@Configuration
	static class Config {

		@Bean
		Integer count() {
			return contextCount.incrementAndGet();
		}
	}

}
