/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to verify claims made in
 * <a href="https://github.com/spring-projects/spring-framework/issues/10796">gh-10796</a>.
 *
 * @author Sam Brannen
 * @author Chris Beams
 * @since 3.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class AutowiredQualifierTests {

	@Autowired
	String foo;

	@Autowired
	@Qualifier("customFoo")
	String customFoo;


	@Test
	void test() {
		assertThat(foo).isEqualTo("normal");
		assertThat(customFoo).isEqualTo("custom");
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		String foo() {
			return "normal";
		}

		@Bean
		String customFoo() {
			return "custom";
		}
	}

}
