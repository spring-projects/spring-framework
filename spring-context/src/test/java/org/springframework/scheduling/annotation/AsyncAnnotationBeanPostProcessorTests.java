/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
public class AsyncAnnotationBeanPostProcessorTests {

	@Test
	public void proxyCreated() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(AsyncAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(AsyncAnnotationBeanPostProcessorTests.TestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
		Object target = context.getBean("target");
		assertTrue(AopUtils.isAopProxy(target));
		context.close();
	}

	@Test
	public void invokedAsynchronously() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(AsyncAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(AsyncAnnotationBeanPostProcessorTests.TestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
		ITestBean testBean = (ITestBean) context.getBean("target");
		testBean.test();
		Thread mainThread = Thread.currentThread();
		testBean.await(3000);
		Thread asyncThread = testBean.getThread();
		assertNotSame(mainThread, asyncThread);
		context.close();
	}

	@Test
	public void threadNamePrefix() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(AsyncAnnotationBeanPostProcessor.class);
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("testExecutor");
		executor.afterPropertiesSet();
		processorDefinition.getPropertyValues().add("executor", executor);
		BeanDefinition targetDefinition = new RootBeanDefinition(AsyncAnnotationBeanPostProcessorTests.TestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
		ITestBean testBean = (ITestBean) context.getBean("target");
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
		ITestBean testBean = (ITestBean) context.getBean("target");
		testBean.test();
		testBean.await(3000);
		Thread asyncThread = testBean.getThread();
		assertTrue(asyncThread.getName().startsWith("testExecutor"));
		context.close();
	}


	private static interface ITestBean {

		Thread getThread();

		void test();

		void await(long timeout);
	}


	public static class TestBean implements ITestBean {

		private Thread thread;

		private final CountDownLatch latch = new CountDownLatch(1);

		public Thread getThread() {
			return this.thread;
		}

		@Async
		public void test() {
			this.thread = Thread.currentThread();
			this.latch.countDown();
		}

		public void await(long timeout) {
			try {
				this.latch.await(timeout, TimeUnit.MILLISECONDS);
			}
			catch (Exception e) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
