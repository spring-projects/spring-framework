/*
 * Copyright 2002-2014 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource}
 * support with an explicitly named properties file that overrides a
 * application-level property configured via
 * {@link PropertySource @PropertySource} on an
 * {@link Configuration @Configuration} class.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestPropertySource("ApplicationPropertyOverridePropertiesFileTestPropertySourceTests.properties")
public class ApplicationPropertyOverridePropertiesFileTestPropertySourceTests {

	@Autowired
	protected Environment env;


	@Test
	public void verifyPropertiesAreAvailableInEnvironment() {
		assertEquals("test override", env.getProperty("explicit"));
	}


	// -------------------------------------------------------------------

	@Configuration
	@PropertySource("classpath:/org/springframework/test/context/env/explicit.properties")
	static class Config {
		/* no user beans required for these tests */
	}

}
