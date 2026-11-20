/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.initializers.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify that a {@link PropertySource} can be set via a
 * custom {@link ApplicationContextInitializer} in the Spring TestContext Framework.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@SpringJUnitConfig(initializers = PropertySourcesInitializerTests.PropertySourceInitializer.class)
class PropertySourcesInitializerTests {

	@Configuration
	static class Config {

		@Value("${enigma}")
		// The following can also be used to directly access the
		// environment instead of relying on placeholder replacement.
		// @Value("#{ environment['enigma'] }")
		private String enigma;


		@Bean
		public String enigma() {
			return enigma;
		}

	}


	@Autowired
	private String enigma;


	@Test
	void customPropertySourceConfiguredViaContextInitializer() {
		assertThat(enigma).isEqualTo("foo");
	}


	public static class PropertySourceInitializer implements
			ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.getEnvironment().getPropertySources().addFirst(
				new MockPropertySource().withProperty("enigma", "foo"));
		}
	}

}
