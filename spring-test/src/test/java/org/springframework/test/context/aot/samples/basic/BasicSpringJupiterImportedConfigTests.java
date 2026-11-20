/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
