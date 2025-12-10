/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
