/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
