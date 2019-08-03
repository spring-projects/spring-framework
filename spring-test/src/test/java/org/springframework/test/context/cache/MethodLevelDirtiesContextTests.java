/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MethodLevelDirtiesContextTests {

	private static final AtomicInteger contextCount = new AtomicInteger();


	@Configuration
	static class Config {

		@Bean
		Integer count() {
			return contextCount.incrementAndGet();
		}
	}


	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private Integer count;


	@Test
	// test## prefix required for @FixMethodOrder.
	public void test01() throws Exception {
		performAssertions(1);
	}

	@Test
	@DirtiesContext(methodMode = BEFORE_METHOD)
	// test## prefix required for @FixMethodOrder.
	public void test02_dirtyContextBeforeTestMethod() throws Exception {
		performAssertions(2);
	}

	@Test
	@DirtiesContext
	// test## prefix required for @FixMethodOrder.
	public void test03_dirtyContextAfterTestMethod() throws Exception {
		performAssertions(2);
	}

	@Test
	// test## prefix required for @FixMethodOrder.
	public void test04() throws Exception {
		performAssertions(3);
	}

	private void performAssertions(int expectedContextCreationCount) throws Exception {
		assertThat(this.context).as("context must not be null").isNotNull();
		assertThat(this.context.isActive()).as("context must be active").isTrue();

		assertThat(this.count).as("count must not be null").isNotNull();
		assertThat(this.count.intValue()).as("count: ").isEqualTo(expectedContextCreationCount);

		assertThat(contextCount.get()).as("context creation count: ").isEqualTo(expectedContextCreationCount);
	}

}
