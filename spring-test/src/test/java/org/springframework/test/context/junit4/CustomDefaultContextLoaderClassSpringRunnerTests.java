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

package org.springframework.test.context.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;
import org.springframework.test.context.support.GenericPropertiesContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which verify that a subclass of {@link DefaultTestContextBootstrapper}
 * can specify a custom <em>default ContextLoader class</em> that overrides the standard
 * default class name.
 *
 * @author Sam Brannen
 * @since 3.0
 */
@RunWith(SpringRunner.class)
@BootstrapWith(CustomDefaultContextLoaderClassSpringRunnerTests.PropertiesBasedTestContextBootstrapper.class)
@ContextConfiguration("PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests-context.properties")
public class CustomDefaultContextLoaderClassSpringRunnerTests {

	@Autowired
	private Pet cat;

	@Autowired
	private String testString;


	@Test
	public void verifyAnnotationAutowiredFields() {
		assertThat(this.cat).as("The cat field should have been autowired.").isNotNull();
		assertThat(this.cat.getName()).isEqualTo("Garfield");

		assertThat(this.testString).as("The testString field should have been autowired.").isNotNull();
		assertThat(this.testString).isEqualTo("Test String");
	}


	public static class PropertiesBasedTestContextBootstrapper extends DefaultTestContextBootstrapper {

		@Override
		protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
			return GenericPropertiesContextLoader.class;
		}
	}

}
