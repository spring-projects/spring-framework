/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @author Sam Brannen
 * @since 5.0
 */
@Configuration
@EnableWebMvc
class WebConfig {

	@Bean
	PersonController personController() {
		return new PersonController();
	}

}
