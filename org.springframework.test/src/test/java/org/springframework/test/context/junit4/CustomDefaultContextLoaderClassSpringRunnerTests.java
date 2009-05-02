/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.test.context.junit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.model.InitializationError;
import org.springframework.beans.Pet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.GenericPropertiesContextLoader;

/**
 * Unit tests which verify that a subclass of {@link SpringJUnit4ClassRunner}
 * can specify a custom <em>default ContextLoader class name</em> that overrides
 * the standard default class name.
 * 
 * @author Sam Brannen
 * @since 3.0
 */
@RunWith(CustomDefaultContextLoaderClassSpringRunnerTests.PropertiesBasedSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests-context.properties")
public class CustomDefaultContextLoaderClassSpringRunnerTests {

	@Autowired
	private Pet cat;

	@Autowired
	private String testString;


	@Test
	public void verifyAnnotationAutowiredFields() {
		assertNotNull("The cat field should have been autowired.", this.cat);
		assertEquals("Garfield", this.cat.getName());

		assertNotNull("The testString field should have been autowired.", this.testString);
		assertEquals("Test String", this.testString);
	}


	public static final class PropertiesBasedSpringJUnit4ClassRunner extends SpringJUnit4ClassRunner {

		public PropertiesBasedSpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
			super(clazz);
		}

		@Override
		protected String getDefaultContextLoaderClassName(Class<?> clazz) {
			return GenericPropertiesContextLoader.class.getName();
		}

	}
}
