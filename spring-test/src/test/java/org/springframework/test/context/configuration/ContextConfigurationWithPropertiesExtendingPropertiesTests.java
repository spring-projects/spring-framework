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

package org.springframework.test.context.configuration;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.junit4.PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests;
import org.springframework.test.context.support.GenericPropertiesContextLoader;
import org.springframework.tests.sample.beans.Pet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which verify that the same custom {@link ContextLoader} can
 * be used at all levels within a test class hierarchy when the
 * {@code loader} is explicitly declared via {@link ContextConfiguration
 * &#064;ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 3.0
 * @see PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests
 * @see ContextConfigurationWithPropertiesExtendingPropertiesAndInheritedLoaderTests
 */
@ContextConfiguration(loader = GenericPropertiesContextLoader.class)
public class ContextConfigurationWithPropertiesExtendingPropertiesTests extends
		PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests {

	@Autowired
	private Pet dog;

	@Autowired
	private String testString2;


	@Test
	public void verifyExtendedAnnotationAutowiredFields() {
		assertThat(this.dog).as("The dog field should have been autowired.").isNotNull();
		assertThat(this.dog.getName()).isEqualTo("Fido");

		assertThat(this.testString2).as("The testString2 field should have been autowired.").isNotNull();
		assertThat(this.testString2).isEqualTo("Test String #2");
	}

}
