/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterImportedConfigTests.ImportedConfig;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses configuration identical to {@link BasicSpringJupiterTests} and
 * {@link BasicSpringJupiterImportedConfigTests} EXCEPT that this class is
 * annotated with {@link Import @Import} to register an additional bean.
 *
 * @author Sam Brannen
 * @since 6.0
 */
@SpringJUnitConfig(BasicTestConfiguration.class)
@Import(ImportedConfig.class)
@TestPropertySource(properties = "test.engine = jupiter")
public class BasicSpringJupiterImportedConfigTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	MessageService messageService;

	@Autowired
	String enigma;

	@Value("${test.engine}")
	String testEngine;

	@org.junit.jupiter.api.Test
	void test() {
		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		assertThat(enigma).isEqualTo("imported!");
		assertThat(testEngine).isEqualTo("jupiter");
		assertThat(context.getEnvironment().getProperty("test.engine"))
			.as("@TestPropertySource").isEqualTo("jupiter");
	}

	@Configuration(proxyBeanMethods = false)
	static class ImportedConfig {

		@Bean
		String enigma() {
			return "imported!";
		}
	}

}
