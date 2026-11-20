/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.hybrid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for hybrid {@link SmartContextLoader} implementations that
 * support path-based and class-based resources simultaneously, as is done in
 * Spring Boot.
 *
 * @author Sam Brannen
 * @since 4.0.4
 * @see HybridContextLoader
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(loader = HybridContextLoader.class)
class HybridContextLoaderTests {

	@Autowired
	String fooFromXml;

	@Autowired
	String fooFromJava;

	@Autowired
	String enigma;


	@Test
	void verifyContentsOfHybridApplicationContext() {
		assertThat(fooFromXml).isEqualTo("XML");
		assertThat(fooFromJava).isEqualTo("Java");

		// Note: the XML bean definition for "enigma" always wins since
		// ConfigurationClassBeanDefinitionReader.isOverriddenByExistingDefinition()
		// lets XML bean definitions override those "discovered" later via a
		// @Bean method.
		assertThat(enigma).isEqualTo("enigma from XML");
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		String fooFromJava() {
			return "Java";
		}

		@Bean
		String enigma() {
			return "enigma from Java";
		}
	}

}
