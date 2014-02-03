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

package org.springframework.test.context.junit4.aci.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.aci.annotation.InitializerWithoutConfigFilesOrClassesTests.EntireAppInitializer;

import static org.junit.Assert.*;

/**
 * Integration test that verifies support for {@link ApplicationContextInitializer
 * ApplicationContextInitializers} in the TestContext framework when the test class
 * declares neither XML configuration files nor annotated configuration classes.
 * 
 * @author Sam Brannen
 * @since 3.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = EntireAppInitializer.class)
public class InitializerWithoutConfigFilesOrClassesTests {

	@Autowired
	private String foo;


	@Test
	public void foo() {
		assertEquals("foo", foo);
	}


	static class EntireAppInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
			new AnnotatedBeanDefinitionReader(applicationContext).register(GlobalConfig.class);
		}
	}

}
