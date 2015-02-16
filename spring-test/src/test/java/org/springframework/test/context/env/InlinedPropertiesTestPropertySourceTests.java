/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.env;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support with
 * inlined properties.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestPropertySource(properties = { "", "foo = bar", "baz quux", "enigma: 42", "x.y.z = a=b=c",
	"server.url = http://example.com", "key.value.1: key=value", "key.value.2 key=value", "key.value.3 key:value" })
public class InlinedPropertiesTestPropertySourceTests {

	@Autowired
	private Environment env;


	@Test
	public void propertiesAreAvailableInEnvironment() {

		// Simple key/value pairs
		assertEquals("bar", env.getProperty("foo"));
		assertEquals("quux", env.getProperty("baz"));
		assertEquals(42, env.getProperty("enigma", Integer.class).intValue());

		// Values containing key/value delimiters (":", "=", " ")
		assertEquals("a=b=c", env.getProperty("x.y.z"));
		assertEquals("http://example.com", env.getProperty("server.url"));
		assertEquals("key=value", env.getProperty("key.value.1"));
		assertEquals("key=value", env.getProperty("key.value.2"));
		assertEquals("key:value", env.getProperty("key.value.3"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void propertyNameOrderingIsPreservedInEnvironment() {
		String[] propertyNames = null;

		ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) env;
		for (PropertySource<?> propertySource : configurableEnvironment.getPropertySources()) {
			if (propertySource instanceof EnumerablePropertySource) {
				EnumerablePropertySource eps = (EnumerablePropertySource) propertySource;
				if (eps.getName().startsWith("test properties")) {
					propertyNames = eps.getPropertyNames();
					break;
				}
			}
		}

		final String[] expectedPropertyNames = new String[] { "foo", "baz", "enigma", "x.y.z", "server.url",
			"key.value.1", "key.value.2", "key.value.3" };

		assertArrayEquals(expectedPropertyNames, propertyNames);
	}


	// -------------------------------------------------------------------

	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

}
