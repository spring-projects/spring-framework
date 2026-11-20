/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit based integration test which verifies that
 * {@link ContextConfiguration @ContextConfiguration} is optional.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.3
 */
@ExtendWith(SpringExtension.class)
class OptionalContextConfigurationTests {

	@Test
	void contextConfigurationAnnotationIsOptional(@Autowired String foo) {
		assertThat(foo).isEqualTo("bar");
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		String foo() {
			return "bar";
		}
	}

}
