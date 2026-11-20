/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.xml;

import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("test-config.xml")
@TestPropertySource(properties = "test.engine = vintage")
@SuppressWarnings("deprecation")
public class XmlSpringVintageTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	MessageService messageService;

	@Value("${test.engine}")
	String testEngine;


	@org.junit.Test
	public void test() {
		assertThat(testEngine)
			.as("@Value").isEqualTo("vintage");
		assertThat(context.getEnvironment().getProperty("test.engine"))
			.as("Environment").isEqualTo("vintage");

		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
	}

}
