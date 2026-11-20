/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.hierarchies.web;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 3.2.2
 */
@SpringJUnitConfig
class EarTests {

	@Configuration(proxyBeanMethods = false)
	static class EarConfig {

		@Bean
		String ear() {
			return "ear";
		}
	}


	// -------------------------------------------------------------------------

	@Autowired
	private ApplicationContext context;

	@Autowired
	private String ear;


	@Test
	void verifyEarConfig() {
		assertThat(context).isNotInstanceOf(WebApplicationContext.class);
		assertThat(context.getParent()).isNull();
		assertThat(ear).isEqualTo("ear");
	}

}
