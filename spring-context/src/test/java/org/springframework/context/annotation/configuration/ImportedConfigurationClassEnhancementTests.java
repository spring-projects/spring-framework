/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.context.annotation.configuration;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.ClassUtils;

import test.beans.TestBean;

/**
 * Unit tests cornering the bug exposed in SPR-6779.
 *
 * @author Chris Beams
 */
public class ImportedConfigurationClassEnhancementTests {


	@Test
	public void autowiredConfigClassIsEnhancedWhenImported() {
		autowiredConfigClassIsEnhanced(ConfigThatDoesImport.class);
	}

	@Test
	public void autowiredConfigClassIsEnhancedWhenRegisteredViaConstructor() {
		autowiredConfigClassIsEnhanced(ConfigThatDoesNotImport.class, ConfigToBeAutowired.class);
	}

	private void autowiredConfigClassIsEnhanced(Class<?>... configClasses) {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(configClasses);
		Config config = ctx.getBean(Config.class);
		assertTrue("autowired config class has not been enhanced",
				ClassUtils.isCglibProxy(config.autowiredConfig));
	}


	@Test
	public void autowiredConfigClassBeanMethodsRespectScopingWhenImported() {
		autowiredConfigClassBeanMethodsRespectScoping(ConfigThatDoesImport.class);
	}

	@Test
	public void autowiredConfigClassBeanMethodsRespectScopingWhenRegisteredViaConstructor() {
		autowiredConfigClassBeanMethodsRespectScoping(ConfigThatDoesNotImport.class, ConfigToBeAutowired.class);
	}

	private void autowiredConfigClassBeanMethodsRespectScoping(Class<?>... configClasses) {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(configClasses);
		Config config = ctx.getBean(Config.class);
		TestBean testBean1 = config.autowiredConfig.testBean();
		TestBean testBean2 = config.autowiredConfig.testBean();
		assertThat("got two distinct instances of testBean when singleton scoping was expected",
				testBean1, sameInstance(testBean2));
	}


	@Test(expected=BeanDefinitionParsingException.class)
	public void importingAnNonConfigurationClassCausesIllegalArgumentException() {
		new AnnotationConfigApplicationContext(ConfigThatImportsNonConfigClass.class);
	}
}

@Configuration
class ConfigToBeAutowired {
	public @Bean TestBean testBean() {
		return new TestBean();
	}
}

class Config {
	@Autowired ConfigToBeAutowired autowiredConfig;
}

@Import(ConfigToBeAutowired.class)
@Configuration
class ConfigThatDoesImport extends Config { }

@Configuration
class ConfigThatDoesNotImport extends Config { }

@Configuration
@Import(Void.class)
class ConfigThatImportsNonConfigClass { }