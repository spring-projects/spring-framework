/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(bfpp.wasCalled).isFalse();
		ac.refresh();
		assertThat(bfpp.wasCalled).isTrue();
	}

	@Test
	public void testDefinedBeanFactoryPostProcessor() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		ac.registerSingleton("bfpp", TestBeanFactoryPostProcessor.class);
		ac.refresh();
		TestBeanFactoryPostProcessor bfpp = (TestBeanFactoryPostProcessor) ac.getBean("bfpp");
		assertThat(bfpp.wasCalled).isTrue();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testMultipleDefinedBeanFactoryPostProcessors() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		MutablePropertyValues pvs1 = new MutablePropertyValues();
		pvs1.add("initValue", "${key}");
		ac.registerSingleton("bfpp1", TestBeanFactoryPostProcessor.class, pvs1);
		MutablePropertyValues pvs2 = new MutablePropertyValues();
		pvs2.add("properties", "key=value");
		ac.registerSingleton("bfpp2", org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.class, pvs2);
		ac.refresh();
		TestBeanFactoryPostProcessor bfpp = (TestBeanFactoryPostProcessor) ac.getBean("bfpp1");
		assertThat(bfpp.initValue).isEqualTo("value");
		assertThat(bfpp.wasCalled).isTrue();
	}

	@Test
	public void testBeanFactoryPostProcessorNotExecutedByBeanFactory() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("tb1", new RootBeanDefinition(TestBean.class));
		bf.registerBeanDefinition("tb2", new RootBeanDefinition(TestBean.class));
		bf.registerBeanDefinition("bfpp", new RootBeanDefinition(TestBeanFactoryPostProcessor.class));
		TestBeanFactoryPostProcessor bfpp = (TestBeanFactoryPostProcessor) bf.getBean("bfpp");
		assertThat(bfpp.wasCalled).isFalse();
	}

	@Test
	public void testBeanDefinitionRegistryPostProcessor() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		ac.addBeanFactoryPostProcessor(new PrioritizedBeanDefinitionRegistryPostProcessor());
		TestBeanDefinitionRegistryPostProcessor bdrpp = new TestBeanDefinitionRegistryPostProcessor();
		ac.addBeanFactoryPostProcessor(bdrpp);
		assertThat(bdrpp.wasCalled).isFalse();
		ac.refresh();
		assertThat(bdrpp.wasCalled).isTrue();
		assertThat(ac.getBean("bfpp1", TestBeanFactoryPostProcessor.class).wasCalled).isTrue();
		assertThat(ac.getBean("bfpp2", TestBeanFactoryPostProcessor.class).wasCalled).isTrue();
	}

	@Test
	public void testBeanDefinitionRegistryPostProcessorRegisteringAnother() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		ac.registerBeanDefinition("bdrpp2", new RootBeanDefinition(OuterBeanDefinitionRegistryPostProcessor.class));
		ac.refresh();
		assertThat(ac.getBean("bfpp1", TestBeanFactoryPostProcessor.class).wasCalled).isTrue();
		assertThat(ac.getBean("bfpp2", TestBeanFactoryPostProcessor.class).wasCalled).isTrue();
	}

	@Test
	public void testPrioritizedBeanDefinitionRegistryPostProcessorRegisteringAnother() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		ac.registerBeanDefinition("bdrpp2", new RootBeanDefinition(PrioritizedOuterBeanDefinitionRegistryPostProcessor.class));
		ac.refresh();
		assertThat(ac.getBean("bfpp1", TestBeanFactoryPostProcessor.class).wasCalled).isTrue();
		assertThat(ac.getBean("bfpp2", TestBeanFactoryPostProcessor.class).wasCalled).isTrue();
	}

	@Test
	public void testBeanFactoryPostProcessorAsApplicationListener() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerBeanDefinition("bfpp", new RootBeanDefinition(ListeningBeanFactoryPostProcessor.class));
		ac.refresh();
		boolean condition = ac.getBean(ListeningBeanFactoryPostProcessor.class).received instanceof ContextRefreshedEvent;
		assertThat(condition).isTrue();
	}

	@Test
	public void testBeanFactoryPostProcessorWithInnerBeanAsApplicationListener() {
		StaticApplicationContext ac = new StaticApplicationContext();
		RootBeanDefinition rbd = new RootBeanDefinition(NestingBeanFactoryPostProcessor.class);
		rbd.getPropertyValues().add("listeningBean", new RootBeanDefinition(ListeningBean.class));
		ac.registerBeanDefinition("bfpp", rbd);
		ac.refresh();
		boolean condition = ac.getBean(NestingBeanFactoryPostProcessor.class).getListeningBean().received instanceof ContextRefreshedEvent;
		assertThat(condition).isTrue();
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


	public static class PrioritizedBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			registry.registerBeanDefinition("bfpp1", new RootBeanDefinition(TestBeanFactoryPostProcessor.class));
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}
	}


	public static class TestBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

		public boolean wasCalled;

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			assertThat(registry.containsBeanDefinition("bfpp1")).isTrue();
			registry.registerBeanDefinition("bfpp2", new RootBeanDefinition(TestBeanFactoryPostProcessor.class));
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			this.wasCalled = true;
		}
	}


	public static class OuterBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			registry.registerBeanDefinition("anotherpp", new RootBeanDefinition(TestBeanDefinitionRegistryPostProcessor.class));
			registry.registerBeanDefinition("ppp", new RootBeanDefinition(PrioritizedBeanDefinitionRegistryPostProcessor.class));
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}
	}


	public static class PrioritizedOuterBeanDefinitionRegistryPostProcessor extends OuterBeanDefinitionRegistryPostProcessor
			implements PriorityOrdered {

		@Override
		public int getOrder() {
			return HIGHEST_PRECEDENCE;
		}
	}


	public static class ListeningBeanFactoryPostProcessor implements BeanFactoryPostProcessor, ApplicationListener<ApplicationEvent> {

		public ApplicationEvent received;

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			Assert.state(this.received == null, "Just one ContextRefreshedEvent expected");
			this.received = event;
		}
	}


	public static class ListeningBean implements ApplicationListener<ApplicationEvent> {

		public ApplicationEvent received;

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			Assert.state(this.received == null, "Just one ContextRefreshedEvent expected");
			this.received = event;
		}
	}


	public static class NestingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

		private ListeningBean listeningBean;

		public void setListeningBean(ListeningBean listeningBean) {
			this.listeningBean = listeningBean;
		}

		public ListeningBean getListeningBean() {
			return listeningBean;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}
	}

}
