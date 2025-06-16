/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.initializers.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.initializers.annotation.InitializerWithoutConfigFilesOrClassesTests.EntireAppInitializer;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies support for {@link ApplicationContextInitializer
 * ApplicationContextInitializers} in the TestContext framework when the test class
 * declares neither XML configuration files nor annotated configuration classes.
 *
 * @author Sam Brannen
 * @since 3.2
 */
@SpringJUnitConfig(initializers = EntireAppInitializer.class)
public class InitializerWithoutConfigFilesOrClassesTests {

	@Autowired
	private String foo;


	@Test
	public void foo() {
		assertThat(foo).isEqualTo("foo");
	}


	static class EntireAppInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
			new AnnotatedBeanDefinitionReader(applicationContext).register(GlobalConfig.class);
		}
	}

}
