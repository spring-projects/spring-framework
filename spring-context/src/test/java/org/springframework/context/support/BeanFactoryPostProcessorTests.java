/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.context.support;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

/**
 * Tests the interaction between {@link ApplicationContext} implementations and
 * any registered {@link BeanFactoryPostProcessor} implementations.  Specifically
 * {@link StaticApplicationContext} is used for the tests, but what's represented
 * here is any {@link AbstractApplicationContext} implementation.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 02.10.2003
 */
public class BeanFactoryPostProcessorTests {

	@Test
	public void testRegisteredBeanFactoryPostProcessor() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		TestBeanFactoryPostProcessor bfpp = new TestBeanFactoryPostProcessor();
		ac.addBeanFactoryPostProcessor(bfpp);
		assertFalse(bfpp.wasCalled);
		ac.refresh();
		assertTrue(bfpp.wasCalled);
	}

	@Test
	public void testDefinedBeanFactoryPostProcessor() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		ac.registerSingleton("bfpp", TestBeanFactoryPostProcessor.class);
		ac.refresh();
		TestBeanFactoryPostProcessor bfpp = (TestBeanFactoryPostProcessor) ac.getBean("bfpp");
		assertTrue(bfpp.wasCalled);
	}

	@Test
	public void testMultipleDefinedBeanFactoryPostProcessors() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		MutablePropertyValues pvs1 = new MutablePropertyValues();
		pvs1.add("initValue", "${key}");
		ac.registerSingleton("bfpp1", TestBeanFactoryPostProcessor.class, pvs1);
		MutablePropertyValues pvs2 = new MutablePropertyValues();
		pvs2.add("properties", "key=value");
		ac.registerSingleton("bfpp2", PropertyPlaceholderConfigurer.class, pvs2);
		ac.refresh();
		TestBeanFactoryPostProcessor bfpp = (TestBeanFactoryPostProcessor) ac.getBean("bfpp1");
		assertEquals("value", bfpp.initValue);
		assertTrue(bfpp.wasCalled);
	}

	@Test
	public void testBeanFactoryPostProcessorNotExecutedByBeanFactory() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("tb1", new RootBeanDefinition(TestBean.class));
		bf.registerBeanDefinition("tb2", new RootBeanDefinition(TestBean.class));
		bf.registerBeanDefinition("bfpp", new RootBeanDefinition(TestBeanFactoryPostProcessor.class));
		TestBeanFactoryPostProcessor bfpp = (TestBeanFactoryPostProcessor) bf.getBean("bfpp");
		assertFalse(bfpp.wasCalled);
	}

	@Test
	public void testBeanDefinitionRegistryPostProcessor() throws Exception {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		TestBeanDefinitionRegistryPostProcessor bdrpp = new TestBeanDefinitionRegistryPostProcessor();
		ac.addBeanFactoryPostProcessor(bdrpp);
		assertFalse(bdrpp.wasCalled);
		ac.refresh();
		assertTrue(bdrpp.wasCalled);
		assertTrue(ac.getBean(TestBeanFactoryPostProcessor.class).wasCalled);
	}

	@Test
	public void testBeanDefinitionRegistryPostProcessorRegisteringAnother() throws Exception {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		ac.registerBeanDefinition("bdrpp2", new RootBeanDefinition(TestBeanDefinitionRegistryPostProcessor2.class));
		ac.refresh();
		assertTrue(ac.getBean(TestBeanFactoryPostProcessor.class).wasCalled);
	}

	public static class TestBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

		public String initValue;

		public void setInitValue(String initValue) {
			this.initValue = initValue;
		}

		public boolean wasCalled = false;

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			wasCalled = true;
		}
	}

	public static class TestBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

		public boolean wasCalled;

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
				throws BeansException {
			registry.registerBeanDefinition("bfpp", new RootBeanDefinition(TestBeanFactoryPostProcessor.class));
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
			this.wasCalled = true;
		}

	}

	public static class TestBeanDefinitionRegistryPostProcessor2 implements BeanDefinitionRegistryPostProcessor {

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
				throws BeansException {
			registry.registerBeanDefinition("anotherpp", new RootBeanDefinition(TestBeanDefinitionRegistryPostProcessor.class));
		}

	}
}
