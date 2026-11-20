/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.bean.override;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.test.context.aot.samples.common.GreetingService;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.2
 */
@ExtendWith(SpringExtension.class)
public class TestBeanJupiterTests {

	@TestBean(methodName = "org.springframework.test.context.aot.samples.bean.override.GreetingServiceFactory#createEnigmaGreetingService")
	GreetingService greetingService;

	@Test
	void test() {
		assertThat(greetingService.greeting()).isEqualTo("enigma");
	}

}
