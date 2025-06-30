/*
 * Copyright 2002-present the original author or authors.
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.SpringProperties;
import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.context.annotation.Bean.Bootstrap.BACKGROUND;
import static org.springframework.core.testfixture.TestGroup.LONG_RUNNING;

/**
 * @author Juergen Hoeller
 * @since 6.2
 */
class BackgroundBootstrapTests {

	@Test
	@Timeout(10)
	@EnabledForTestGroups(LONG_RUNNING)
	void bootstrapWithUnmanagedThread() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(UnmanagedThreadBeanConfig.class);
		ctx.getBean("testBean1", TestBean.class);
		ctx.getBean("testBean2", TestBean.class);
		ctx.close();
	}

	@Test
	@Timeout(10)
	@EnabledForTestGroups(LONG_RUNNING)
	void bootstrapWithUnmanagedThreads() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(UnmanagedThreadsBeanConfig.class);
		ctx.getBean("testBean1", TestBean.class);
		ctx.getBean("testBean2", TestBean.class);
		ctx.getBean("testBean3", TestBean.class);
		ctx.getBean("testBean4", TestBean.class);
		ctx.close();
	}

	@Test
	@Timeout(10)
	@EnabledForTestGroups(LONG_RUNNING)
	void bootstrapWithStrictLockingFlag() {
		SpringProperties.setFlag(DefaultListableBeanFactory.STRICT_LOCKING_PROPERTY_NAME);
		try {
			ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(StrictLockingBeanConfig.class);
			assertThat(ctx.getBean("testBean2", TestBean.class).getSpouse()).isSameAs(ctx.getBean("testBean1"));
			ctx.close();
		}
		finally {
			SpringProperties.setProperty(DefaultListableBeanFactory.STRICT_LOCKING_PROPERTY_NAME, null);
		}
	}

	@Test
	@Timeout(10)
	@EnabledForTestGroups(LONG_RUNNING)
	void bootstrapWithStrictLockingInferred() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(InferredLockingBeanConfig.class);
		ExecutorService threadPool = Executors.newFixedThreadPool(2);
		threadPool.submit(() -> ctx.refresh());
		Thread.sleep(500);
		threadPool.submit(() -> ctx.getBean("testBean2"));
		Thread.sleep(1000);
		assertThat(ctx.getBean("testBean2", TestBean.class).getSpouse()).isSameAs(ctx.getBean("testBean1"));
		ctx.close();
	}

	@Test
	@Timeout(10)
	@EnabledForTestGroups(LONG_RUNNING)
	void bootstrapWithStrictLockingTurnedOff() throws InterruptedException {
		SpringProperties.setFlag(DefaultListableBeanFactory.STRICT_LOCKING_PROPERTY_NAME, false);
		try {
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.register(InferredLockingBeanConfig.class);
			ExecutorService threadPool = Executors.newFixedThreadPool(2);
			threadPool.submit(() -> ctx.refresh());
			Thread.sleep(500);
			threadPool.submit(() -> ctx.getBean("testBean2"));
			Thread.sleep(1000);
			assertThat(ctx.getBean("testBean2", TestBean.class).getSpouse()).isNull();
			ctx.close();
		}
		finally {
			SpringProperties.setProperty(DefaultListableBeanFactory.STRICT_LOCKING_PROPERTY_NAME, null);
		}
	}

	@Test
	@Timeout(10)
	@EnabledForTestGroups(LONG_RUNNING)
	void bootstrapWithCircularReferenceAgainstMainThread() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(CircularReferenceAgainstMainThreadBeanConfig.class);
		ctx.getBean("testBean1", TestBean.class);
		ctx.getBean("testBean2", TestBean.class);
		ctx.close();
	}

	@Test
	@Timeout(10)
	@EnabledForTestGroups(LONG_RUNNING)
	void bootstrapWithCircularReferenceWithBlockingMainThread() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new AnnotationConfigApplicationContext(CircularReferenceWithBlockingMainThreadBeanConfig.class))
				.withRootCauseInstanceOf(BeanCurrentlyInCreationException.class);
	}

	@Test
	@Timeout(10)
	@EnabledForTestGroups(LONG_RUNNING)
	void bootstrapWithCircularReferenceInSameThread() {
		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> new AnnotationConfigApplicationContext(CircularReferenceInSameThreadBeanConfig.class))
				.withRootCauseInstanceOf(BeanCurrentlyInCreationException.class);
	}

	@Test
	@Timeout(10)
	@EnabledForTestGroups(LONG_RUNNING)
	void bootstrapWithCircularReferenceInMultipleThreads() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new AnnotationConfigApplicationContext(CircularReferenceInMultipleThreadsBeanConfig.class))
				.withRootCauseInstanceOf(BeanCurrentlyInCreationException.class);
	}

	@Test
	@Timeout(10)
	@EnabledForTestGroups(LONG_RUNNING)
	void bootstrapWithCustomExecutor() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(CustomExecutorBeanConfig.class);
		ctx.getBean("testBean1", TestBean.class);
		ctx.getBean("testBean2", TestBean.class);
		ctx.getBean("testBean3", TestBean.class);
		ctx.getBean("testBean4", TestBean.class);
		ctx.close();
	}

	@Test
	@Timeout(10)
	@EnabledForTestGroups(LONG_RUNNING)
	void bootstrapWithCustomExecutorAndStrictLocking() {
		SpringProperties.setFlag(DefaultListableBeanFactory.STRICT_LOCKING_PROPERTY_NAME);
		try {
			ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(CustomExecutorBeanConfig.class);
			ctx.getBean("testBean1", TestBean.class);
			ctx.getBean("testBean2", TestBean.class);
			ctx.getBean("testBean3", TestBean.class);
			ctx.getBean("testBean4", TestBean.class);
			ctx.close();
		}
		finally {
			SpringProperties.setProperty(DefaultListableBeanFactory.STRICT_LOCKING_PROPERTY_NAME, null);
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class UnmanagedThreadBeanConfig {

		@Bean
		public TestBean testBean1(ObjectProvider<TestBean> testBean2) {
			new Thread(testBean2::getObject).start();
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean();
		}

		@Bean
		public TestBean testBean2() {
			try {
				Thread.sleep(2000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean();
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class UnmanagedThreadsBeanConfig {

		@Bean
		public TestBean testBean1(ObjectProvider<TestBean> testBean3, ObjectProvider<TestBean> testBean4) {
			new Thread(testBean3::getObject).start();
			new Thread(testBean4::getObject).start();
			new Thread(testBean3::getObject).start();
			new Thread(testBean4::getObject).start();
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean();
		}

		@Bean
		public TestBean testBean2(TestBean testBean4) {
			return new TestBean(testBean4);
		}

		@Bean
		public TestBean testBean3(TestBean testBean4) {
			return new TestBean(testBean4);
		}

		@Bean
		public FactoryBean<TestBean> testBean4() {
			try {
				Thread.sleep(2000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			TestBean testBean = new TestBean();
			return new FactoryBean<>() {
				@Override
				public TestBean getObject() {
					return testBean;
				}
				@Override
				public Class<?> getObjectType() {
					return testBean.getClass();
				}
			};
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class StrictLockingBeanConfig {

		@Bean
		public TestBean testBean1(ObjectProvider<TestBean> testBean2) {
			new Thread(testBean2::getObject).start();
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean("testBean1");
		}

		@Bean
		public TestBean testBean2(ConfigurableListableBeanFactory beanFactory) {
			return new TestBean((TestBean) beanFactory.getSingleton("testBean1"));
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class InferredLockingBeanConfig {

		@Bean
		public TestBean testBean1() {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean("testBean1");
		}

		@Bean
		public TestBean testBean2(ConfigurableListableBeanFactory beanFactory) {
			return new TestBean((TestBean) beanFactory.getSingleton("testBean1"));
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class CircularReferenceAgainstMainThreadBeanConfig {

		@Bean
		public TestBean testBean1(ObjectProvider<TestBean> testBean2) {
			new Thread(testBean2::getObject).start();
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean();
		}

		@Bean
		public TestBean testBean2(TestBean testBean1) {
			try {
				Thread.sleep(2000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean();
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class CircularReferenceWithBlockingMainThreadBeanConfig {

		@Bean
		public TestBean testBean1(ObjectProvider<TestBean> testBean2) {
			new Thread(testBean2::getObject).start();
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean(testBean2.getObject());
		}

		@Bean
		public TestBean testBean2(ObjectProvider<TestBean> testBean1) {
			try {
				Thread.sleep(2000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean(testBean1.getObject());
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class CircularReferenceInSameThreadBeanConfig {

		@Bean
		public TestBean testBean1(ObjectProvider<TestBean> testBean2) {
			new Thread(testBean2::getObject).start();
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean();
		}

		@Bean
		public TestBean testBean2(TestBean testBean3) {
			try {
				Thread.sleep(2000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean();
		}

		@Bean
		public TestBean testBean3(TestBean testBean2) {
			return new TestBean();
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class CircularReferenceInMultipleThreadsBeanConfig {

		@Bean
		public TestBean testBean1(ObjectProvider<TestBean> testBean2, ObjectProvider<TestBean> testBean3,
				ObjectProvider<TestBean> testBean4) {

			new Thread(testBean2::getObject).start();
			new Thread(testBean3::getObject).start();
			new Thread(testBean4::getObject).start();
			try {
				Thread.sleep(3000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean();
		}

		@Bean
		public TestBean testBean2(ObjectProvider<TestBean> testBean3) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean(testBean3.getObject());
		}

		@Bean
		public TestBean testBean3(ObjectProvider<TestBean> testBean4) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean(testBean4.getObject());
		}

		@Bean
		public TestBean testBean4(ObjectProvider<TestBean> testBean2) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new TestBean(testBean2.getObject());
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class CustomExecutorBeanConfig {

		@Bean
		public ThreadPoolTaskExecutor bootstrapExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setThreadNamePrefix("Custom-");
			executor.setCorePoolSize(2);
			executor.initialize();
			return executor;
		}

		@Bean(bootstrap = BACKGROUND) @DependsOn("testBean3")
		public TestBean testBean1(TestBean testBean3) throws InterruptedException {
			Thread.sleep(6000);
			return new TestBean();
		}

		@Bean(bootstrap = BACKGROUND) @Lazy
		public TestBean testBean2() throws InterruptedException {
			Thread.sleep(6000);
			return new TestBean();
		}

		@Bean @Lazy
		public TestBean testBean3() {
			return new TestBean();
		}

		@Bean
		public TestBean testBean4(@Lazy TestBean testBean1, @Lazy TestBean testBean2, @Lazy TestBean testBean3) {
			return new TestBean();
		}
	}

}
