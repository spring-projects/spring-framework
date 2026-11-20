/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.web;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 3.2
 */
@SpringJUnitConfig
class BasicAnnotationConfigWacTests extends AbstractBasicWacTests {

	@Autowired
	ServletContextAwareBean servletContextAwareBean;

	@Test
	void fooEnigmaAutowired() {
		assertThat(foo).isEqualTo("enigma");
	}

	@Test
	void servletContextAwareBeanProcessed() {
		assertThat(servletContextAwareBean).isNotNull();
		assertThat(servletContextAwareBean.getServletContext()).isNotNull();
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		String foo() {
			return "enigma";
		}

		@Bean
		ServletContextAwareBean servletContextAwareBean() {
			return new ServletContextAwareBean();
		}

	}

}
