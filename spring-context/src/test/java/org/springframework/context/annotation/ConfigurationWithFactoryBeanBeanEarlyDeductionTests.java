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

package org.springframework.context.annotation;

import java.util.Arrays;

import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link AbstractBeanFactory} type inference from
 * {@link FactoryBean FactoryBeans} defined in the configuration.
 *
 * @author Phillip Webb
 */
public class ConfigurationWithFactoryBeanBeanEarlyDeductionTests {

	@Test
	public void preFreezeDirect() {
		assertPreFreeze(DirectConfiguration.class);
	}

	@Test
	public void postFreezeDirect() {
		assertPostFreeze(DirectConfiguration.class);
	}

	@Test
	public void preFreezeGenericMethod() {
		assertPreFreeze(GenericMethodConfiguration.class);
	}

	@Test
	public void postFreezeGenericMethod() {
		assertPostFreeze(GenericMethodConfiguration.class);
	}

	@Test
	public void preFreezeGenericClass() {
		assertPreFreeze(GenericClassConfiguration.class);
	}

	@Test
	public void postFreezeGenericClass() {
		assertPostFreeze(GenericClassConfiguration.class);
	}

	private void assertPostFreeze(Class<?> configurationClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configurationClass);
		assertContainsMyBeanName(context);
	}

	private void assertPreFreeze(Class<?> configurationClass,
			BeanFactoryPostProcessor... postProcessors) {
		NameCollectingBeanFactoryPostProcessor postProcessor = new NameCollectingBeanFactoryPostProcessor();
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		Arrays.stream(postProcessors).forEach(context::addBeanFactoryPostProcessor);
		context.addBeanFactoryPostProcessor(postProcessor);
		context.register(configurationClass);
		context.refresh();
		assertContainsMyBeanName(postProcessor.getNames());
	}

	private void assertContainsMyBeanName(AnnotationConfigApplicationContext context) {
		assertContainsMyBeanName(context.getBeanNamesForType(MyBean.class, true, false));
	}

	private void assertContainsMyBeanName(String[] names) {
		assertThat(names).containsExactly("myBean");
	}

	private static class NameCollectingBeanFactoryPostProcessor
			implements BeanFactoryPostProcessor {

		private String[] names;

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
			this.names = beanFactory.getBeanNamesForType(MyBean.class, true, false);
		}

		public String[] getNames() {
			return this.names;
		}

	}

	@Configuration
	static class DirectConfiguration {

		@Bean
		MyBean myBean() {
			return new MyBean();
		}

	}

	@Configuration
	static class GenericMethodConfiguration {

		@Bean
		FactoryBean<MyBean> myBean() {
			return new TestFactoryBean<>(new MyBean());
		}

	}

	@Configuration
	static class GenericClassConfiguration {

		@Bean
		MyFactoryBean myBean() {
			return new MyFactoryBean();
		}

	}

	static class MyBean {
	}

	static class TestFactoryBean<T> implements FactoryBean<T> {

		private final T instance;

		public TestFactoryBean(T instance) {
			this.instance = instance;
		}

		@Override
		public T getObject() throws Exception {
			return this.instance;
		}

		@Override
		public Class<?> getObjectType() {
			return this.instance.getClass();
		}

	}

	static class MyFactoryBean extends TestFactoryBean<MyBean> {

		public MyFactoryBean() {
			super(new MyBean());
		}

	}

}
