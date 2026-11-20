/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.basic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.samples.basic.BasicSpringTestNGTests.CustomInitializer;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@ContextConfiguration(classes = BasicTestConfiguration.class, initializers = CustomInitializer.class)
public class BasicSpringTestNGTests extends AbstractTestNGSpringContextTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	MessageService messageService;

	@Value("${test.engine}")
	String testEngine;

	@org.testng.annotations.Test
	public void test() {
		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		assertThat(testEngine).isEqualTo("testng");
		assertThat(context.getEnvironment().getProperty("test.engine"))
			.as("@TestPropertySource").isEqualTo("testng");
	}

	public static class CustomInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.getEnvironment().getPropertySources()
				.addFirst(new MockPropertySource().withProperty("test.engine", "testng"));
		}

	}

}
