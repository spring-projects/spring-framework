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

package org.springframework.beans.factory.aot;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.Ordered;
import org.springframework.core.test.io.support.MockSpringFactoriesLoader;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BeanDefinitionMethodGeneratorFactory}.
 *
 * @author Phillip Webb
 */
class BeanDefinitionMethodGeneratorFactoryTests {

	@Test
	void createWhenBeanRegistrationExcludeFilterBeanIsNotAotProcessorThrowsException() {
		BeanRegistrationExcludeFilter filter = registeredBean -> false;
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("filter", filter);
		assertThatIllegalStateException()
				.isThrownBy(() -> new BeanDefinitionMethodGeneratorFactory(beanFactory))
				.withMessageContaining("also implement an AOT processor interface");
	}

	@Test
	void createWhenBeanRegistrationExcludeFilterFactoryIsNotAotProcessorLoads() {
		BeanRegistrationExcludeFilter filter = registeredBean -> false;
		MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();
		loader.addInstance(BeanRegistrationExcludeFilter.class, filter);
		assertThatNoException().isThrownBy(() -> new BeanDefinitionMethodGeneratorFactory(
				AotServices.factories(loader)));
	}

	@Test
	void getBeanDefinitionMethodGeneratorWhenExcludedByBeanRegistrationExcludeFilterReturnsNull() {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		springFactoriesLoader.addInstance(BeanRegistrationExcludeFilter.class,
				new MockBeanRegistrationExcludeFilter(true, 0));
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
				AotServices.factoriesAndBeans(springFactoriesLoader, beanFactory));
		assertThat(methodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean)).isNull();
	}

	@Test
	void getBeanDefinitionMethodGeneratorWhenExcludedByBeanRegistrationExcludeFilterBeanReturnsNull() {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		beanFactory.registerSingleton("filter",
				new MockBeanRegistrationExcludeFilter(true, 0));
		BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
				AotServices.factoriesAndBeans(springFactoriesLoader, beanFactory));
		assertThat(methodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean)).isNull();
	}

	@Test
	void getBeanDefinitionMethodGeneratorConsidersFactoryLoadedExcludeFiltersAndBeansInOrderedOrder() {
		MockBeanRegistrationExcludeFilter filter1 = new MockBeanRegistrationExcludeFilter(false, 1);
		MockBeanRegistrationExcludeFilter filter2 = new MockBeanRegistrationExcludeFilter(false, 2);
		MockBeanRegistrationExcludeFilter filter3 = new MockBeanRegistrationExcludeFilter(false, 3);
		MockBeanRegistrationExcludeFilter filter4 = new MockBeanRegistrationExcludeFilter(true, 4);
		MockBeanRegistrationExcludeFilter filter5 = new MockBeanRegistrationExcludeFilter(true, 5);
		MockBeanRegistrationExcludeFilter filter6 = new MockBeanRegistrationExcludeFilter(true, 6);
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.addInstance(BeanRegistrationExcludeFilter.class, filter3, filter1, filter5);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("filter4", filter4);
		beanFactory.registerSingleton("filter2", filter2);
		beanFactory.registerSingleton("filter6", filter6);
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
				AotServices.factoriesAndBeans(springFactoriesLoader, beanFactory));
		assertThat(methodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean)).isNull();
		assertThat(filter1.wasCalled()).isTrue();
		assertThat(filter2.wasCalled()).isTrue();
		assertThat(filter3.wasCalled()).isTrue();
		assertThat(filter4.wasCalled()).isTrue();
		assertThat(filter5.wasCalled()).isFalse();
		assertThat(filter6.wasCalled()).isFalse();
	}

	@Test
	void getBeanDefinitionMethodGeneratorAddsContributionsFromProcessors() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanRegistrationAotContribution beanContribution = mock();
		BeanRegistrationAotProcessor processorBean = registeredBean -> beanContribution;
		beanFactory.registerSingleton("processorBean", processorBean);
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		BeanRegistrationAotContribution loaderContribution = mock();
		BeanRegistrationAotProcessor loaderProcessor = registeredBean -> loaderContribution;
		springFactoriesLoader.addInstance(BeanRegistrationAotProcessor.class,
				loaderProcessor);
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
				AotServices.factoriesAndBeans(springFactoriesLoader, beanFactory));
		BeanDefinitionMethodGenerator methodGenerator = methodGeneratorFactory
				.getBeanDefinitionMethodGenerator(registeredBean);
		assertThat(methodGenerator).extracting("aotContributions").asInstanceOf(LIST)
				.containsExactly(beanContribution, loaderContribution);
	}

	@Test
	void getBeanDefinitionMethodGeneratorWhenRegisteredBeanIsAotProcessorFiltersBean() {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("test1", BeanDefinitionBuilder
				.rootBeanDefinition(TestBeanFactoryInitializationAotProcessorBean.class).getBeanDefinition());
		RegisteredBean registeredBean1 = RegisteredBean.of(beanFactory, "test1");
		beanFactory.registerBeanDefinition("test2", BeanDefinitionBuilder
				.rootBeanDefinition(TestBeanRegistrationAotProcessorBean.class).getBeanDefinition());
		RegisteredBean registeredBean2 = RegisteredBean.of(beanFactory, "test2");
		BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
				AotServices.factoriesAndBeans(springFactoriesLoader, beanFactory));
		assertThat(methodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean1)).isNull();
		assertThat(methodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean2)).isNull();
	}

	@Test
	void getBeanDefinitionMethodGeneratorWhenRegisteredBeanIsAotProcessorAndIsNotExcludedAndBeanRegistrationExcludeFilterDoesNotFilterBean() {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("test", BeanDefinitionBuilder
				.rootBeanDefinition(TestBeanRegistrationAotProcessorAndNotExcluded.class).getBeanDefinition());
		RegisteredBean registeredBean1 = RegisteredBean.of(beanFactory, "test");
		BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
				AotServices.factoriesAndBeans(springFactoriesLoader, beanFactory));
		assertThat(methodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean1)).isNotNull();
	}

	private RegisteredBean registerTestBean(DefaultListableBeanFactory beanFactory) {
		beanFactory.registerBeanDefinition("test", BeanDefinitionBuilder
				.rootBeanDefinition(TestBean.class).getBeanDefinition());
		return RegisteredBean.of(beanFactory, "test");
	}


	static class MockBeanRegistrationExcludeFilter implements
			BeanRegistrationAotProcessor, BeanRegistrationExcludeFilter, Ordered {

		private final boolean excluded;

		private final int order;

		@Nullable
		private RegisteredBean registeredBean;

		MockBeanRegistrationExcludeFilter(boolean excluded, int order) {
			this.excluded = excluded;
			this.order = order;
		}

		@Override
		public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
			return null;
		}

		@Override
		public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
			this.registeredBean = registeredBean;
			return this.excluded;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		boolean wasCalled() {
			return this.registeredBean != null;
		}

	}

	static class TestBean {

	}

	static class TestBeanFactoryInitializationAotProcessorBean implements BeanFactoryInitializationAotProcessor {

		@Override
		public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
			return null;
		}

	}

	static class TestBeanRegistrationAotProcessorBean implements BeanRegistrationAotProcessor {

		@Override
		public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
			return null;
		}

	}

	static class TestBeanRegistrationAotProcessorAndNotExcluded
			extends TestBeanRegistrationAotProcessorBean {

		@Override
		public boolean isBeanExcludedFromAotProcessing() {
			return false;
		}

	}

	@SuppressWarnings("unused")
	static class InnerTestBean {

	}

}
