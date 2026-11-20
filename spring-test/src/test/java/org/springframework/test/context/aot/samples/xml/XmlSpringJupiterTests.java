/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.xml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@SpringJUnitConfig(locations = "test-config.xml")
@TestPropertySource(properties = "test.engine = jupiter")
public class XmlSpringJupiterTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	MessageService messageService;

	@Value("${test.engine}")
	String testEngine;


	@org.junit.jupiter.api.Test
	void test() {
		assertThat(testEngine)
			.as("@Value").isEqualTo("jupiter");
		assertThat(context.getEnvironment().getProperty("test.engine"))
			.as("Environment").isEqualTo("jupiter");

		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
	}

}
