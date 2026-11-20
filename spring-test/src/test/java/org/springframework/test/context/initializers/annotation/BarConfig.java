/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.initializers.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Sam Brannen
 * @since 4.3
 */
@Configuration(proxyBeanMethods = false)
class BarConfig {

	@Bean
	String bar() {
		return "bar";
	}

}
