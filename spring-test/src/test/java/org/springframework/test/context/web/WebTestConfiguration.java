/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.web;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

/**
 * Custom <em>composed annotation</em> combining {@link WebAppConfiguration} and
 * {@link ContextConfiguration} as meta-annotations.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@WebAppConfiguration
@ContextConfiguration(classes = FooConfig.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebTestConfiguration {
}

@Configuration(proxyBeanMethods = false)
class FooConfig {

	@Bean
	public String foo() {
		return "enigma";
	}

}
