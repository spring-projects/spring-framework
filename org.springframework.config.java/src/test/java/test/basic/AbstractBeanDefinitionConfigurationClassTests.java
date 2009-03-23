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
package test.basic;

import static org.junit.Assert.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.config.java.Bean;
import org.springframework.config.java.Configuration;
import org.springframework.config.java.support.ConfigurationClassPostProcessor;


/**
 * Covers the somewhat unlilely case of a {@link Configuration} class being declared
 * as an abstract {@link BeanDefinition}.
 * 
 * @author Chris Beams
 * @see BeanDefinition#isAbstract()
 */
public class AbstractBeanDefinitionConfigurationClassTests {

	@SuppressWarnings("unused")
	@Test
	public void abstractConfigurationClassBeanDefinitionsAreIgnored() {
		@Configuration class Abstract { @Bean Object foo1() { return null; } }
		@Configuration class Concrete { @Bean Object foo2() { return null; } }

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("abstract",
				rootBeanDefinition(Abstract.class).setAbstract(true).getBeanDefinition());
		factory.registerBeanDefinition("concrete",
				rootBeanDefinition(Concrete.class).setAbstract(false).getBeanDefinition());
		new ConfigurationClassPostProcessor().postProcessBeanFactory(factory);

		assertTrue("abstract configuration should be CGLIB-enhanced",
				AopUtils.isCglibProxyClassName(factory.getBeanDefinition("abstract").getBeanClassName()));
		assertTrue("concrete configuration should be CGLIB-enhanced",
				AopUtils.isCglibProxyClassName(factory.getBeanDefinition("concrete").getBeanClassName()));

		assertFalse("abstract configuration's @Bean method should not be registered",
				factory.containsBeanDefinition("foo1"));
		assertTrue("concrete configuration's @Bean method should be registered",
				factory.containsBeanDefinition("foo2"));
	}
}
