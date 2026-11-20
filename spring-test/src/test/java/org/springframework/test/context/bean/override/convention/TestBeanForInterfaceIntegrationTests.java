/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.convention;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.TestBeanFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestBean} that use bean factory methods defined
 * in implemented interfaces.
 *
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
class TestBeanForInterfaceIntegrationTests implements TestBeanFactory {

	@TestBean(methodName = "createTestMessage")
	String message;


	@Test
	void test() {
		assertThat(message).isEqualTo("test");
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		String message() {
			return "prod";
		}
	}

}
