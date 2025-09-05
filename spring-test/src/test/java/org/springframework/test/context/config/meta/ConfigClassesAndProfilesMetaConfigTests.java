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
