/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.meta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for meta-annotation attribute override support, demonstrating
 * that the test class is used as the <em>declaring class</em> when detecting default
 * configuration classes for the declaration of {@code @ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@ExtendWith(SpringExtension.class)
@ConfigClassesAndProfilesMetaConfig(profiles = "dev")
class ConfigClassesAndProfilesMetaConfigTests {

	@Autowired
	String foo;


	@Test
	void foo() {
		assertThat(foo).isEqualTo("Local Dev Foo");
	}


	@Configuration(proxyBeanMethods = false)
	@Profile("dev")
	static class DevConfig {

		@Bean
		String foo() {
			return "Local Dev Foo";
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Profile("prod")
	static class ProductionConfig {

		@Bean
		String foo() {
			return "Local Production Foo";
		}
	}

}
