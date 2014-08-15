/*
 * Copyright 2002-2014 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class AsyncAnnotationBeanPostProcessorTests {

	@Test
	public void proxyCreated() {
		ConfigurableApplicationContext context = initContext(
				new RootBeanDefinition(AsyncAnnotationBeanPostProcessor.class));
		Object target = context.getBean("target");
		assertTrue(AopUtils.isAopProxy(target));
		context.close();
	}

	@Test
	public void invokedAsynchronously() {
		ConfigurableApplicationContext context = initContext(
				new RootBeanDefinition(AsyncAnnotationBeanPostProcessor.class));
		ITestBean testBean = context.getBean("target", ITestBean.class);
		testBean.test();
		Thread mainThread = Thread.currentThread();
		testBean.await(3000);
		Thread asyncThread = testBean.getThread();
		assertNotSame(mainThread, asyncThread);
		context.close();
	}

	@Test
	public void threadNamePrefix() {
		BeanDefinition processorDefinition = new RootBeanDefinition(AsyncAnnotationBeanPostProcessor.class);
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("testExecutor");
		executor.afterPropertiesSet();
		processorDefinition.getPropertyValues().add("executor", executor);
		ConfigurableApplicationContext context = initContext(processorDefinition);
		ITestBean testBean = context.getBean("target", ITestBean.class);
		testBean.test();
		testBean.await(3000);
		Thread asyncThread = testBean.getThread();
		assertTrue(asyncThread.getName().startsWith("testExecutor"));
		context.close();
	}

	@Test
	public void configuredThroughNamespace() {
		GenericXmlApplicationContext context = new GenericXmlApplicationContext();
		context.load(new ClassPathResource("taskNamespaceTests.xml", getClass()));
		context.refresh();
		ITestBean testBean = context.getBean("target", ITestBean.class);
		testBean.test();
		testBean.await(3000);
		Thread asyncThread = testBean.getThread();
		assertTrue(asyncThread.getName().startsWith("testExecutor"));

		TestableAsyncUncaughtExceptionHandler exceptionHandler =
				context.getBean("exceptionHandler", TestableAsyncUncaughtExceptionHandler.class);
		assertFalse("handler should not have been called yet", exceptionHandler.isCalled());

		testBean.failWithVoid();
		exceptionHandler.await(3000);
		Method m = ReflectionUtils.findMethod(TestBean.class, "failWithVoid");
		exceptionHandler.assertCalledWith(m, UnsupportedOperationException.class);
		context.close();
	}

	@Test
	public void handleExceptionWithFuture() {
		ConfigurableApplicationContext context = initContext(
				new RootBeanDefinition(AsyncAnnotationBeanPostProcessor.class));
		ITestBean testBean = context.getBean("target", ITestBean.class);
		final Future<Object> result = testBean.failWithFuture();

		try {
			result.get();
		}
		catch (InterruptedException ex) {
			fail("Should not have failed with InterruptedException: " + ex);
		}
		catch (ExecutionException ex) {
			// expected
			assertEquals("Wrong exception cause", UnsupportedOperationException.class, ex.getCause().getClass());
		}
	}

	@Test
	public void handleExceptionWithCustomExceptionHandler() {
		Method m = ReflectionUtils.findMethod(TestBean.class, "failWithVoid");
		TestableAsyncUncaughtExceptionHandler exceptionHandler =
				new TestableAsyncUncaughtExceptionHandler();
		BeanDefinition processorDefinition = new RootBeanDefinition(AsyncAnnotationBeanPostProcessor.class);
		processorDefinition.getPropertyValues().add("exceptionHandler", exceptionHandler);

		ConfigurableApplicationContext context = initContext(processorDefinition);
		ITestBean testBean = context.getBean("target", ITestBean.class);

		assertFalse("Handler should not have been called", exceptionHandler.isCalled());
		testBean.failWithVoid();
		exceptionHandler.await(3000);
		exceptionHandler.assertCalledWith(m, UnsupportedOperationException.class);
	}

	@Test
	public void exceptionHandlerThrowsUnexpectedException() {
		Method m = ReflectionUtils.findMethod(TestBean.class, "failWithVoid");
		TestableAsyncUncaughtExceptionHandler exceptionHandler =
				new TestableAsyncUncaughtExceptionHandler(true);
		BeanDefinition processorDefinition = new RootBeanDefinition(AsyncAnnotationBeanPostProcessor.class);
		processorDefinition.getPropertyValues().add("exceptionHandler", exceptionHandler);
		processorDefinition.getPropertyValues().add("executor", new DirectExecutor());

		ConfigurableApplicationContext context = initContext(processorDefinition);
		ITestBean testBean = context.getBean("target", ITestBean.class);

		assertFalse("Handler should not have been called", exceptionHandler.isCalled());
		try {
			testBean.failWithVoid();
			exceptionHandler.assertCalledWith(m, UnsupportedOperationException.class);
		}
		catch (Exception e) {
			fail("No unexpected exception should have been received");
		}
	}

	private ConfigurableApplicationContext initContext(BeanDefinition asyncAnnotationBeanPostProcessorDefinition) {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition targetDefinition =
				new RootBeanDefinition(AsyncAnnotationBeanPostProcessorTests.TestBean.class);
		context.registerBeanDefinition("postProcessor", asyncAnnotationBeanPostProcessorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
		return context;
	}


	private static interface ITestBean {

		Thread getThread();

		void test();

		Future<Object> failWithFuture();

		void failWithVoid();

		void await(long timeout);
	}


	public static class TestBean implements ITestBean {

		private Thread thread;

		private final CountDownLatch latch = new CountDownLatch(1);

		@Override
		public Thread getThread() {
			return this.thread;
		}

		@Override
		@Async
		public void test() {
			this.thread = Thread.currentThread();
			this.latch.countDown();
		}

		@Async
		public Future<Object> failWithFuture() {
			throw new UnsupportedOperationException("failWithFuture");
		}

		@Async
		public void failWithVoid() {
			throw new UnsupportedOperationException("failWithVoid");
		}

		@Override
		public void await(long timeout) {
			try {
				this.latch.await(timeout, TimeUnit.MILLISECONDS);
			}
			catch (Exception e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private static class DirectExecutor implements Executor {

		@Override
		public void execute(Runnable r) {
			r.run();
		}
	}

}
