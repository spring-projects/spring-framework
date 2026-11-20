/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.xml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@ContextConfiguration("test-config.xml")
@TestPropertySource(properties = "test.engine = testng")
public class XmlSpringTestNGTests extends AbstractTestNGSpringContextTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	MessageService messageService;

	@Value("${test.engine}")
	String testEngine;


	@org.testng.annotations.Test
	public void test() {
		assertThat(testEngine)
			.as("@Value").isEqualTo("testng");
		assertThat(context.getEnvironment().getProperty("test.engine"))
			.as("Environment").isEqualTo("testng");

		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
	}

}
