/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.initializers.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Sam Brannen
 * @since 3.2
 */
@Configuration(proxyBeanMethods = false)
class GlobalConfig {

	@Bean
	public String foo() {
		return "foo";
	}

	@Bean
	public String baz() {
		return "global config";
	}

}
