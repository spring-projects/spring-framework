/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * Integration tests that verify detection of a default properties file
 * when {@link TestPropertySource @TestPropertySource} is <em>empty</em>.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestPropertySource
public class DefaultPropertiesFileDetectionTestPropertySourceTests {

	@Autowired
	protected Environment env;


	@Test
	public void verifyPropertiesAreAvailableInEnvironment() {
		// from DefaultPropertiesFileDetectionTestPropertySourceTests.properties
		assertEnvironmentValue("riddle", "auto detected");
	}

	protected void assertEnvironmentValue(String key, String expected) {
		assertEquals("Value of key [" + key + "].", expected, env.getProperty(key));
	}


	// -------------------------------------------------------------------

	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

}
