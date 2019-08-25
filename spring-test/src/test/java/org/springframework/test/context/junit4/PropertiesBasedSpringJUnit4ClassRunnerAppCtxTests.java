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

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.GenericPropertiesContextLoader;
import org.springframework.tests.sample.beans.Pet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * JUnit 4 based test class, which verifies the expected functionality of
 * {@link SpringRunner} in conjunction with support for application contexts
 * loaded from Java {@link Properties} files. Specifically, the
 * {@link ContextConfiguration#loader() loader} attribute of {@code ContextConfiguration}
 * and the
 * {@link org.springframework.test.context.support.GenericPropertiesContextLoader#getResourceSuffix()
 * resourceSuffix} property of {@code GenericPropertiesContextLoader} are tested.
 * </p>
 * <p>
 * Since no {@link ContextConfiguration#locations() locations} are explicitly defined, the
 * {@code resourceSuffix} is set to &quot;-context.properties&quot;, and since default
 * resource locations will be detected by default, this test class's dependencies will be
 * injected via {@link Autowired annotation-based autowiring} from beans defined in the
 * {@link ApplicationContext} loaded from the default classpath resource: &quot;
 * {@code /org/springframework/test/junit4/PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests-context.properties}
 * &quot;.
 * </p>
 *
 * @author Sam Brannen
 * @since 2.5
 * @see GenericPropertiesContextLoader
 * @see SpringJUnit4ClassRunnerAppCtxTests
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(loader = GenericPropertiesContextLoader.class)
public class PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests {

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

}
