/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.junit4.aci.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify that a {@link PropertySource} can be set via a
 * custom {@link ApplicationContextInitializer} in the Spring TestContext Framework.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = PropertySourcesInitializerTests.PropertySourceInitializer.class)
public class PropertySourcesInitializerTests {

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
	public void customPropertySourceConfiguredViaContextInitializer() {
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
