/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.env;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.support.TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support with
 * inlined properties.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see InlinedPropertiesWithTextBlockTestPropertySourceTests
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@TestPropertySource(properties = { "", "foo = bar", "baz quux", "enigma: 42", "x.y.z = a=b=c",
	"server.url = https://example.com", "key.value.1: key=value", "key.value.2 key=value", "key.value.3 key:value" })
class InlinedPropertiesTestPropertySourceTests {

	@Autowired
	ConfigurableEnvironment env;


	@Test
	void propertiesAreAvailableInEnvironment() {
		// Simple key/value pairs
		assertEnvironmentProperty("foo", "bar");
		assertEnvironmentProperty("baz", "quux");
		assertEnvironmentProperty("enigma", "42");

		// Values containing key/value delimiters (":", "=", " ")
		assertEnvironmentProperty("x.y.z", "a=b=c");
		assertEnvironmentProperty("server.url", "https://example.com");
		assertEnvironmentProperty("key.value.1", "key=value");
		assertEnvironmentProperty("key.value.2", "key=value");
		assertEnvironmentProperty("key.value.3", "key:value");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void propertyNameOrderingIsPreservedInEnvironment() {
		EnumerablePropertySource eps = (EnumerablePropertySource) env.getPropertySources().get(
			INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
		assertThat(eps.getPropertyNames()).containsExactly("foo", "baz", "enigma", "x.y.z", "server.url",
				"key.value.1", "key.value.2", "key.value.3" );
	}

	private void assertEnvironmentProperty(String name, Object value) {
		assertThat(this.env.getProperty(name)).as("environment property '%s'", name).isEqualTo(value);
	}


	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

}
