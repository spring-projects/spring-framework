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

package org.springframework.test.context.aot.samples.basic;

import org.junit.jupiter.api.Test;

import org.springframework.aot.AotDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.aot.samples.common.DefaultMessageService;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DisabledInAotMode} class-level tests.
 *
 * <p>This test class differs from {@link DisabledInAotProcessingTests} whose
 * {@code ApplicationContext} will simply fail during AOT processing. Whereas,
 * the {@code ApplicationContext} for this test class can be properly processed
 * for AOT optimizations, but we want to ensure that we can also disable such a
 * test class in AOT mode if desired.
 *
 * @author Sam Brannen
 * @since 6.1
 * @see DisabledInAotRuntimeMethodLevelTests
 * @see DisabledInAotProcessingTests
 */
@SpringJUnitConfig
@TestPropertySource(properties = "disabledInAotMode = class-level")
@DisabledInAotMode
public class DisabledInAotRuntimeClassLevelTests {

	@Test
	void test(@Autowired MessageService messageService,
			@Value("${disabledInAotMode}") String disabledInAotMode) {

		assertThat(AotDetector.useGeneratedArtifacts()).as("Should be disabled in AOT mode").isFalse();
		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		assertThat(disabledInAotMode).isEqualTo("class-level");
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		MessageService defaultMessageService() {
			return new DefaultMessageService();
		}
	}

}
