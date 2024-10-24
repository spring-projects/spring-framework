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

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.when;

/**
 * Tests for {@link MockitoBean @MockitoBean}, {@link SpringMethodRule}, and
 * {@link Repeat @Repeat} with JUnit 4.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/27693">gh-27693</a>
 */
public class MockitoBeanAndSpringMethodRuleWithRepeatJUnit4IntegrationTests {

	private static int invocations;

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@MockitoBean
	Service service;

	@BeforeClass
	public static void beforeClass() {
		invocations = 0;
	}

	@Test
	@Repeat(2)
	public void repeatedTest() {
		assertThat(service.greeting()).as("mock should have been reset").isNull();

		when(service.greeting()).thenReturn("test");
		assertThat(service.greeting()).isEqualTo("test");

		invocations++;
	}

	@AfterClass
	public static void afterClass() {
		assertThat(invocations).isEqualTo(2);
	}


	interface Service {

		String greeting();
	}

}
