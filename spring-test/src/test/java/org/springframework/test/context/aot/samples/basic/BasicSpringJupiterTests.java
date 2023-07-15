/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.aot.samples.basic;

import org.junit.jupiter.api.Nested;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterTests.DummyTestExecutionListener;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.aot.samples.management.ManagementConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@SpringJUnitConfig({BasicTestConfiguration.class, ManagementConfiguration.class})
@TestExecutionListeners(listeners = DummyTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@TestPropertySource(properties = "test.engine = jupiter")
public class BasicSpringJupiterTests {

	@org.junit.jupiter.api.Test
	void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
			@Value("${test.engine}") String testEngine) {
		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		assertThat(testEngine).isEqualTo("jupiter");
		assertThat(context.getEnvironment().getProperty("test.engine"))
			.as("@TestPropertySource").isEqualTo("jupiter");
	}

	@Nested
	@TestPropertySource(properties = "foo=bar")
	@ActiveProfiles(resolver = SpanishActiveProfilesResolver.class)
	public class NestedTests {

		@org.junit.jupiter.api.Test
		void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
				@Value("${test.engine}") String testEngine, @Value("${foo}") String foo) {
			assertThat(messageService.generateMessage()).isEqualTo("¡Hola, AOT!");
			assertThat(foo).isEqualTo("bar");
			assertThat(testEngine).isEqualTo("jupiter");
			assertThat(context.getEnvironment().getProperty("test.engine"))
				.as("@TestPropertySource").isEqualTo("jupiter");
		}

	}

	public static class DummyTestExecutionListener extends AbstractTestExecutionListener {
	}

}

