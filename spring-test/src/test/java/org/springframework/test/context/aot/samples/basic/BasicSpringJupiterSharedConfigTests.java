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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.aot.samples.management.ManagementConfiguration;
import org.springframework.test.context.env.YamlTestProperties;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses configuration identical to {@link BasicSpringJupiterTests}.
 *
 * <p>NOTE: if you modify the configuration for this test class, you must also
 * modify the configuration for {@link BasicSpringJupiterTests} accordingly.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see BasicSpringJupiterTests
 */
@SpringJUnitConfig({BasicTestConfiguration.class, ManagementConfiguration.class})
@TestPropertySource(properties = "test.engine = jupiter")
// We cannot use `classpath*:` in AOT tests until gh-31088 is resolved.
// @YamlTestProperties("classpath*:**/aot/samples/basic/test?.yaml")
@YamlTestProperties({
	"classpath:org/springframework/test/context/aot/samples/basic/test1.yaml",
	"classpath:org/springframework/test/context/aot/samples/basic/test2.yaml"
})
public class BasicSpringJupiterSharedConfigTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	MessageService messageService;

	@Value("${test.engine}")
	String testEngine;

	@org.junit.jupiter.api.Test
	void test() {
		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		assertThat(testEngine).isEqualTo("jupiter");
		BasicSpringJupiterTests.assertEnvProperties(context);
	}

}
