/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.aot.AotServices.Source;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.test.io.support.MockSpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AotServices}.
 *
 * @author Phillip Webb
 */
class AotServicesTests {

	@Test
	void factoriesLoadsFromAotFactoriesFiles() {
		AotServices<?> loaded = AotServices.factories()
				.load(BeanFactoryInitializationAotProcessor.class);
		assertThat(loaded)
				.anyMatch(BeanFactoryInitializationAotProcessor.class::isInstance);
	}

	@Test
	void factoriesWithClassLoaderLoadsFromAotFactoriesFile() {
		TestSpringFactoriesClassLoader classLoader = new TestSpringFactoriesClassLoader(
				"aot-services.factories");
		AotServices<?> loaded = AotServices.factories(classLoader)
				.load(TestService.class);
		assertThat(loaded).anyMatch(TestServiceImpl.class::isInstance);
	}

	@Test
	void factoriesWithSpringFactoriesLoaderWhenSpringFactoriesLoaderIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AotServices.factories((SpringFactoriesLoader) null))
				.withMessage("'springFactoriesLoader' must not be null");
	}

	@Test
	void factoriesWithSpringFactoriesLoaderLoadsFromSpringFactoriesLoader() {
		MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();
		loader.addInstance(TestService.class, new TestServiceImpl());
		AotServices<?> loaded = AotServices.factories(loader).load(TestService.class);
		assertThat(loaded).anyMatch(TestServiceImpl.class::isInstance);
	}

	@Test
	void factoriesAndBeansWhenBeanFactoryIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AotServices.factoriesAndBeans(null))
				.withMessage("'beanFactory' must not be null");
	}

	@Test
	void factoriesAndBeansLoadsFromFactoriesAndBeanFactory() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.setBeanClassLoader(
				new TestSpringFactoriesClassLoader("aot-services.factories"));
		beanFactory.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		AotServices<?> loaded = AotServices.factoriesAndBeans(beanFactory).load(TestService.class);
		assertThat(loaded).anyMatch(TestServiceImpl.class::isInstance);
		assertThat(loaded).anyMatch(TestBean.class::isInstance);
	}

	@Test
	void factoriesAndBeansWithSpringFactoriesLoaderLoadsFromSpringFactoriesLoaderAndBeanFactory() {
		MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();
		loader.addInstance(TestService.class, new TestServiceImpl());
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		AotServices<?> loaded = AotServices.factoriesAndBeans(loader, beanFactory).load(TestService.class);
		assertThat(loaded).anyMatch(TestServiceImpl.class::isInstance);
		assertThat(loaded).anyMatch(TestBean.class::isInstance);
	}

	@Test
	void factoriesAndBeansWithSpringFactoriesLoaderWhenSpringFactoriesLoaderIsNullThrowsException() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AotServices.factoriesAndBeans(null, beanFactory))
				.withMessage("'springFactoriesLoader' must not be null");
	}

	@Test
	void iteratorReturnsServicesIterator() {
		AotServices<?> loaded = AotServices
				.factories(new TestSpringFactoriesClassLoader("aot-services.factories"))
				.load(TestService.class);
		assertThat(loaded.iterator().next()).isInstanceOf(TestServiceImpl.class);
	}

	@Test
	void streamReturnsServicesStream() {
		AotServices<?> loaded = AotServices
				.factories(new TestSpringFactoriesClassLoader("aot-services.factories"))
				.load(TestService.class);
		assertThat(loaded.stream()).anyMatch(TestServiceImpl.class::isInstance);
	}

	@Test
	void asListReturnsServicesList() {
		AotServices<?> loaded = AotServices
				.factories(new TestSpringFactoriesClassLoader("aot-services.factories"))
				.load(TestService.class);
		assertThat(loaded.asList()).anyMatch(TestServiceImpl.class::isInstance);
	}

	@Test
	void findByBeanNameWhenMatchReturnsService() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		AotServices<?> loaded = AotServices.factoriesAndBeans(beanFactory).load(TestService.class);
		assertThat(loaded.findByBeanName("test")).isInstanceOf(TestBean.class);
	}

	@Test
	void findByBeanNameWhenNoMatchReturnsNull() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		AotServices<?> loaded = AotServices.factoriesAndBeans(beanFactory).load(TestService.class);
		assertThat(loaded.findByBeanName("missing")).isNull();
	}

	@Test
	void loadLoadsFromBeanFactoryAndSpringFactoriesLoaderInOrder() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("b1", new TestServiceImpl(0, "b1"));
		beanFactory.registerSingleton("b2", new TestServiceImpl(2, "b2"));
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.addInstance(TestService.class,
				new TestServiceImpl(1, "l1"));
		springFactoriesLoader.addInstance(TestService.class,
				new TestServiceImpl(3, "l2"));
		Iterable<TestService> loaded = AotServices
				.factoriesAndBeans(springFactoriesLoader, beanFactory)
				.load(TestService.class);
		assertThat(loaded).map(Object::toString).containsExactly("b1", "l1", "b2", "l2");
	}

	@Test
	void getSourceReturnsSource() {
		MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();
		loader.addInstance(TestService.class, new TestServiceImpl());
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		AotServices<TestService> loaded = AotServices.factoriesAndBeans(loader, beanFactory).load(TestService.class);
		assertThat(loaded.getSource(loaded.asList().get(0))).isEqualTo(Source.SPRING_FACTORIES_LOADER);
		assertThat(loaded.getSource(loaded.asList().get(1))).isEqualTo(Source.BEAN_FACTORY);
		TestService missing = mock(TestService.class);
		assertThatIllegalStateException().isThrownBy(()->loaded.getSource(missing));
	}

	@Test
	void getSourceWhenMissingThrowsException() {
		AotServices<TestService> loaded = AotServices.factories().load(TestService.class);
		TestService missing = mock(TestService.class);
		assertThatIllegalStateException().isThrownBy(()->loaded.getSource(missing));
	}

	interface TestService {
	}


	static class TestServiceImpl implements TestService, Ordered {

		private final int order;

		private final String name;


		TestServiceImpl() {
			this(0, "test");
		}

		TestServiceImpl(int order, String name) {
			this.order = order;
			this.name = name;
		}


		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public String toString() {
			return this.name;
		}

	}


	static class TestBean implements TestService {

	}


	static class TestSpringFactoriesClassLoader extends ClassLoader {

		private final String factoriesName;


		TestSpringFactoriesClassLoader(String factoriesName) {
			super(Thread.currentThread().getContextClassLoader());
			this.factoriesName = factoriesName;
		}


		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			return (!"META-INF/spring/aot.factories".equals(name))
					? super.getResources(name)
					: super.getResources("org/springframework/beans/factory/aot/"
							+ this.factoriesName);
		}

	}

}
