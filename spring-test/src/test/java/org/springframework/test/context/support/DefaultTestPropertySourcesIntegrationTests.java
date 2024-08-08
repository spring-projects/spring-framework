/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which ensure that test-related property sources are not
 * registered by default.
 *
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
@DirtiesContext
class DefaultTestPropertySourcesIntegrationTests {

	@Autowired
	ConfigurableEnvironment env;


	@Test
	void ensureTestRelatedPropertySourcesAreNotRegisteredByDefault() {
		assertPropertySourceIsNotRegistered(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
		assertPropertySourceIsNotRegistered(DynamicValuesPropertySource.PROPERTY_SOURCE_NAME);
	}

	private void assertPropertySourceIsNotRegistered(String name) {
		MutablePropertySources propertySources = this.env.getPropertySources();
		assertThat(propertySources.contains(name))
				.as("PropertySource \"%s\" should not be registered by default", name)
				.isFalse();
	}


	@Configuration
	static class Config {
	}

}
