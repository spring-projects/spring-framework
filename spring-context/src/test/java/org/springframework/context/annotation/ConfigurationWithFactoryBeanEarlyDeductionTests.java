/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link AbstractBeanFactory} type inference from
 * {@link FactoryBean FactoryBeans} defined in the configuration.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
class ConfigurationWithFactoryBeanEarlyDeductionTests {

	@Test
	void preFreezeDirect() {
		assertPreFreeze(DirectConfiguration.class);
	}

	@Test
	void postFreezeDirect() {
		assertPostFreeze(DirectConfiguration.class);
	}

	@Test
	void preFreezeGenericMethod() {
		assertPreFreeze(GenericMethodConfiguration.class);
	}

	@Test
	void postFreezeGenericMethod() {
		assertPostFreeze(GenericMethodConfiguration.class);
	}

	@Test
	void preFreezeGenericClass() {
		assertPreFreeze(GenericClassConfiguration.class);
	}

	@Test
	void postFreezeGenericClass() {
		assertPostFreeze(GenericClassConfiguration.class);
	}

	@Test
	void preFreezeAttribute() {
		assertPreFreeze(AttributeClassConfiguration.class);
	}

	@Test
	void postFreezeAttribute() {
		assertPostFreeze(AttributeClassConfiguration.class);
	}

	@Test
	void preFreezeTargetType() {
		assertPreFreeze(TargetTypeConfiguration.class);
	}

	@Test
	void postFreezeTargetType() {
		assertPostFreeze(TargetTypeConfiguration.class);
	}

	@Test
	void preFreezeUnresolvedGenericFactoryBean() {
		// Covers the case where a @Configuration is picked up via component scanning
		// and its bean definition only has a String bean class. In such cases
		// beanDefinition.hasBeanClass() returns false so we need to actually
		// call determineTargetType ourselves
		GenericBeanDefinition factoryBeanDefinition = new GenericBeanDefinition();
		factoryBeanDefinition.setBeanClassName(GenericClassConfiguration.class.getName());
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(FactoryBean.class);
		beanDefinition.setFactoryBeanName("factoryBean");
		beanDefinition.setFactoryMethodName("myBean");
		GenericApplicationContext context = new GenericApplicationContext();
		try {
			context.registerBeanDefinition("factoryBean", factoryBeanDefinition);
			context.registerBeanDefinition("myBean", beanDefinition);
			NameCollectingBeanFactoryPostProcessor postProcessor = new NameCollectingBeanFactoryPostProcessor();
			context.addBeanFactoryPostProcessor(postProcessor);
			context.refresh();
			assertContainsMyBeanName(postProcessor.getNames());
		}
		finally {
			context.close();
		}
	}


	private void assertPostFreeze(Class<?> configurationClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configurationClass);
		assertContainsMyBeanName(context);
	}

	private void assertPreFreeze(Class<?> configurationClass, BeanFactoryPostProcessor... postProcessors) {
		NameCollectingBeanFactoryPostProcessor postProcessor = new NameCollectingBeanFactoryPostProcessor();
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		try {
			Arrays.stream(postProcessors).forEach(context::addBeanFactoryPostProcessor);
			context.addBeanFactoryPostProcessor(postProcessor);
			context.register(configurationClass);
			context.refresh();
			assertContainsMyBeanName(postProcessor.getNames());
		}
		finally {
			context.close();
		}
	}

	private void assertContainsMyBeanName(AnnotationConfigApplicationContext context) {
		assertContainsMyBeanName(context.getBeanNamesForType(MyBean.class, true, false));
	}

	private void assertContainsMyBeanName(String[] names) {
		assertThat(names).containsExactly("myBean");
	}


	private static class NameCollectingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

		private String[] names;

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			ResolvableType typeToMatch = ResolvableType.forClassWithGenerics(MyBean.class, String.class);
			this.names = beanFactory.getBeanNamesForType(typeToMatch, true, false);
		}

		public String[] getNames() {
			return this.names;
		}
	}

	@Configuration
	static class DirectConfiguration {

		@Bean
		MyBean<String> myBean() {
			return new MyBean<>();
		}
	}

	@Configuration
	static class GenericMethodConfiguration {

		@Bean
		FactoryBean<MyBean<String>> myBean() {
			return new TestFactoryBean<>(new MyBean<>());
		}
	}

	@Configuration
	static class GenericClassConfiguration {

		@Bean
		MyFactoryBean myBean() {
			return new MyFactoryBean();
		}
	}

	@Configuration
	@Import(AttributeClassRegistrar.class)
	static class AttributeClassConfiguration {
	}

	static class AttributeClassRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
			BeanDefinition definition = BeanDefinitionBuilder.genericBeanDefinition(
					RawWithAbstractObjectTypeFactoryBean.class).getBeanDefinition();
			definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE,
					ResolvableType.forClassWithGenerics(MyBean.class, String.class));
			registry.registerBeanDefinition("myBean", definition);
		}
	}

	@Configuration
	@Import(TargetTypeRegistrar.class)
	static class TargetTypeConfiguration {
	}

	static class TargetTypeRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
			RootBeanDefinition definition = new RootBeanDefinition(RawWithAbstractObjectTypeFactoryBean.class);
			definition.setTargetType(ResolvableType.forClassWithGenerics(FactoryBean.class,
					ResolvableType.forClassWithGenerics(MyBean.class, String.class)));
			definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE,
					ResolvableType.forClassWithGenerics(MyBean.class, String.class));
			registry.registerBeanDefinition("myBean", definition);
		}
	}

	abstract static class AbstractMyBean {
	}

	static class MyBean<T> extends AbstractMyBean {
	}

	static class TestFactoryBean<T> implements FactoryBean<T> {

		private final T instance;

		public TestFactoryBean(T instance) {
			this.instance = instance;
		}

		@Override
		public T getObject() {
			return this.instance;
		}

		@Override
		public Class<?> getObjectType() {
			return this.instance.getClass();
		}
	}

	static class MyFactoryBean extends TestFactoryBean<MyBean<String>> {

		public MyFactoryBean() {
			super(new MyBean<>());
		}
	}

	static class RawWithAbstractObjectTypeFactoryBean implements FactoryBean<Object> {

		@Override
		public Object getObject() throws Exception {
			throw new IllegalStateException();
		}

		@Override
		public Class<?> getObjectType() {
			return MyBean.class;
		}
	}

}
