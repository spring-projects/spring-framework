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
