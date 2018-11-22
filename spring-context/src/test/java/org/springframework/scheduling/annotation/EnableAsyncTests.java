/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.scheduling.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

/**
 * Tests use of @EnableAsync on @Configuration classes.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 */
public class EnableAsyncTests {

	@Test
	public void proxyingOccurs() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AsyncConfig.class);
		ctx.refresh();

		AsyncBean asyncBean = ctx.getBean(AsyncBean.class);
		assertThat(AopUtils.isAopProxy(asyncBean), is(true));
		asyncBean.work();
	}

	@Test
	public void proxyingOccursWithMockitoStub() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AsyncConfigWithMockito.class, AsyncBeanUser.class);
		ctx.refresh();

		AsyncBeanUser asyncBeanUser = ctx.getBean(AsyncBeanUser.class);
		AsyncBean asyncBean = asyncBeanUser.getAsyncBean();
		assertThat(AopUtils.isAopProxy(asyncBean), is(true));
		asyncBean.work();
	}

	@Test
	public void properExceptionForExistingProxyDependencyMismatch() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AsyncConfig.class, AsyncBeanWithInterface.class, AsyncBeanUser.class);

		try {
			ctx.refresh();
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException ex) {
			assertTrue(ex.getCause() instanceof BeanNotOfRequiredTypeException);
		}
	}

	@Test
	public void properExceptionForResolvedProxyDependencyMismatch() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AsyncConfig.class, AsyncBeanUser.class, AsyncBeanWithInterface.class);

		try {
			ctx.refresh();
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException ex) {
			assertTrue(ex.getCause() instanceof BeanNotOfRequiredTypeException);
		}
	}

	@Test
	public void withAsyncBeanWithExecutorQualifiedByName() throws ExecutionException, InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AsyncWithExecutorQualifiedByNameConfig.class);
		ctx.refresh();

		AsyncBeanWithExecutorQualifiedByName asyncBean = ctx.getBean(AsyncBeanWithExecutorQualifiedByName.class);
		Future<Thread> workerThread0 = asyncBean.work0();
		assertThat(workerThread0.get().getName(), not(anyOf(startsWith("e1-"), startsWith("otherExecutor-"))));
		Future<Thread> workerThread = asyncBean.work();
		assertThat(workerThread.get().getName(), startsWith("e1-"));
		Future<Thread> workerThread2 = asyncBean.work2();
		assertThat(workerThread2.get().getName(), startsWith("otherExecutor-"));
		Future<Thread> workerThread3 = asyncBean.work3();
		assertThat(workerThread3.get().getName(), startsWith("otherExecutor-"));
	}

	@Test
	public void asyncProcessorIsOrderedLowestPrecedenceByDefault() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AsyncConfig.class);
		ctx.refresh();

		AsyncAnnotationBeanPostProcessor bpp = ctx.getBean(AsyncAnnotationBeanPostProcessor.class);
		assertThat(bpp.getOrder(), is(Ordered.LOWEST_PRECEDENCE));
	}

	@Test
	public void orderAttributeIsPropagated() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(OrderedAsyncConfig.class);
		ctx.refresh();

		AsyncAnnotationBeanPostProcessor bpp = ctx.getBean(AsyncAnnotationBeanPostProcessor.class);
		assertThat(bpp.getOrder(), is(Ordered.HIGHEST_PRECEDENCE));
	}

	@Test
	public void customAsyncAnnotationIsPropagated() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(CustomAsyncAnnotationConfig.class, CustomAsyncBean.class);
		ctx.refresh();

		Object bean = ctx.getBean(CustomAsyncBean.class);
		assertTrue(AopUtils.isAopProxy(bean));
		boolean isAsyncAdvised = false;
		for (Advisor advisor : ((Advised) bean).getAdvisors()) {
			if (advisor instanceof AsyncAnnotationAdvisor) {
				isAsyncAdvised = true;
				break;
			}
		}
		assertTrue("bean was not async advised as expected", isAsyncAdvised);
	}

	/**
	 * Fails with classpath errors on trying to classload AnnotationAsyncExecutionAspect
	 */
	@Test(expected = BeanDefinitionStoreException.class)
	public void aspectModeAspectJAttemptsToRegisterAsyncAspect() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AspectJAsyncAnnotationConfig.class);
		ctx.refresh();
	}

	@Test
	public void customExecutorBean() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(CustomExecutorBean.class);
		ctx.refresh();

		AsyncBean asyncBean = ctx.getBean(AsyncBean.class);
		asyncBean.work();
		Thread.sleep(500);
		assertThat(asyncBean.getThreadOfExecution().getName(), startsWith("Custom-"));

		ctx.close();
	}

	@Test
	public void customExecutorConfig() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(CustomExecutorConfig.class);
		ctx.refresh();

		AsyncBean asyncBean = ctx.getBean(AsyncBean.class);
		asyncBean.work();
		Thread.sleep(500);
		assertThat(asyncBean.getThreadOfExecution().getName(), startsWith("Custom-"));

		TestableAsyncUncaughtExceptionHandler exceptionHandler = (TestableAsyncUncaughtExceptionHandler)
				ctx.getBean("exceptionHandler");
		assertFalse("handler should not have been called yet", exceptionHandler.isCalled());

		asyncBean.fail();
		Thread.sleep(500);
		Method method = ReflectionUtils.findMethod(AsyncBean.class, "fail");
		exceptionHandler.assertCalledWith(method, UnsupportedOperationException.class);

		ctx.close();
	}

	@Test  // SPR-14949
	public void findOnInterfaceWithInterfaceProxy() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Spr14949ConfigA.class);

		AsyncInterface asyncBean = ctx.getBean(AsyncInterface.class);
		asyncBean.work();
		Thread.sleep(500);
		assertThat(asyncBean.getThreadOfExecution().getName(), startsWith("Custom-"));

		ctx.close();
	}

	@Test  // SPR-14949
	public void findOnInterfaceWithCglibProxy() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Spr14949ConfigB.class);

		AsyncInterface asyncBean = ctx.getBean(AsyncInterface.class);
		asyncBean.work();
		Thread.sleep(500);
		assertThat(asyncBean.getThreadOfExecution().getName(), startsWith("Custom-"));

		ctx.close();
	}


	static class AsyncBeanWithExecutorQualifiedByName {

		@Async
		public Future<Thread> work0() {
			return new AsyncResult<>(Thread.currentThread());
		}

		@Async("e1")
		public Future<Thread> work() {
			return new AsyncResult<>(Thread.currentThread());
		}

		@Async("otherExecutor")
		public Future<Thread> work2() {
			return new AsyncResult<>(Thread.currentThread());
		}

		@Async("e2")
		public Future<Thread> work3() {
			return new AsyncResult<>(Thread.currentThread());
		}
	}


	static class AsyncBean {

		private Thread threadOfExecution;

		@Async
		public void work() {
			this.threadOfExecution = Thread.currentThread();
		}

		@Async
		public void fail() {
			throw new UnsupportedOperationException();
		}

		public Thread getThreadOfExecution() {
			return threadOfExecution;
		}
	}


	@Component("asyncBean")
	static class AsyncBeanWithInterface extends AsyncBean implements Runnable {

		@Override
		public void run() {
		}
	}


	static class AsyncBeanUser {

		private final AsyncBean asyncBean;

		public AsyncBeanUser(AsyncBean asyncBean) {
			this.asyncBean = asyncBean;
		}

		public AsyncBean getAsyncBean() {
			return asyncBean;
		}
	}


	@EnableAsync(annotation = CustomAsync.class)
	static class CustomAsyncAnnotationConfig {
	}


	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface CustomAsync {
	}


	static class CustomAsyncBean {

		@CustomAsync
		public void work() {
		}
	}


	@Configuration
	@EnableAsync(order = Ordered.HIGHEST_PRECEDENCE)
	static class OrderedAsyncConfig {

		@Bean
		public AsyncBean asyncBean() {
			return new AsyncBean();
		}
	}


	@Configuration
	@EnableAsync(mode = AdviceMode.ASPECTJ)
	static class AspectJAsyncAnnotationConfig {

		@Bean
		public AsyncBean asyncBean() {
			return new AsyncBean();
		}
	}


	@Configuration
	@EnableAsync
	static class AsyncConfig {

		@Bean
		public AsyncBean asyncBean() {
			return new AsyncBean();
		}
	}


	@Configuration
	@EnableAsync
	static class AsyncConfigWithMockito {

		@Bean
		@Lazy
		public AsyncBean asyncBean() {
			return Mockito.mock(AsyncBean.class);
		}
	}


	@Configuration
	@EnableAsync
	static class AsyncWithExecutorQualifiedByNameConfig {

		@Bean
		public AsyncBeanWithExecutorQualifiedByName asyncBean() {
			return new AsyncBeanWithExecutorQualifiedByName();
		}

		@Bean
		public Executor e1() {
			return new ThreadPoolTaskExecutor();
		}

		@Bean
		@Qualifier("e2")
		public Executor otherExecutor() {
			return new ThreadPoolTaskExecutor();
		}
	}


	@Configuration
	@EnableAsync
	static class CustomExecutorBean {

		@Bean
		public AsyncBean asyncBean() {
			return new AsyncBean();
		}

		@Bean
		public Executor taskExecutor() {
			return Executors.newSingleThreadExecutor(new CustomizableThreadFactory("Custom-"));
		}
	}


	@Configuration
	@EnableAsync
	static class CustomExecutorConfig implements AsyncConfigurer {

		@Bean
		public AsyncBean asyncBean() {
			return new AsyncBean();
		}

		@Override
		public Executor getAsyncExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setThreadNamePrefix("Custom-");
			executor.initialize();
			return executor;
		}

		@Override
		public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
			return exceptionHandler();
		}

		@Bean
		public AsyncUncaughtExceptionHandler exceptionHandler() {
			return new TestableAsyncUncaughtExceptionHandler();
		}
	}


	public interface AsyncInterface {

		@Async
		void work();

		Thread getThreadOfExecution();
	}


	public static class AsyncService implements AsyncInterface {

		private Thread threadOfExecution;

		@Override
		public void work() {
			this.threadOfExecution = Thread.currentThread();
		}

		@Override
		public Thread getThreadOfExecution() {
			return threadOfExecution;
		}
	}


	@Configuration
	@EnableAsync
	static class Spr14949ConfigA implements AsyncConfigurer {

		@Bean
		public AsyncInterface asyncBean() {
			return new AsyncService();
		}

		@Override
		public Executor getAsyncExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setThreadNamePrefix("Custom-");
			executor.initialize();
			return executor;
		}

		@Override
		public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
			return null;
		}
	}


	@Configuration
	@EnableAsync(proxyTargetClass = true)
	static class Spr14949ConfigB implements AsyncConfigurer {

		@Bean
		public AsyncInterface asyncBean() {
			return new AsyncService();
		}

		@Override
		public Executor getAsyncExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setThreadNamePrefix("Custom-");
			executor.initialize();
			return executor;
		}

		@Override
		public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
			return null;
		}
	}

}
