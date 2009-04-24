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

package org.springframework.context.annotation.configuration;

import java.lang.annotation.Inherited;

import org.junit.Test;
import test.beans.TestBean;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;

/**
 * Tests that polymorphic Configuration classes need not explicitly redeclare the
 * {@link Configuration} annotation. This respects the {@link Inherited} nature
 * of the Configuration annotation, even though it's being detected via ASM.
 *
 * @author Chris Beams
 */
public class PolymorphicConfigurationTests {

	@Test
	public void beanMethodsDetectedOnSuperClass() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(Config.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.getBean("testBean", TestBean.class);
	}


	@Configuration
	static class SuperConfig {

		@Bean
		public TestBean testBean() {
			return new TestBean();
		}
	}


	@Configuration
	static class Config extends SuperConfig {
	}

}
