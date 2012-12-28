/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.scheduling.config;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CustomizableThreadCreator;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
public class ExecutorBeanDefinitionParserTests {

	private ApplicationContext context;


	@Before
	public void setup() {
		this.context = new ClassPathXmlApplicationContext(
				"executorContext.xml", ExecutorBeanDefinitionParserTests.class);
	}

	@Test
	public void defaultExecutor() throws Exception {
		Object executor = this.context.getBean("default");
		assertEquals(1, getCorePoolSize(executor));
		assertEquals(Integer.MAX_VALUE, getMaxPoolSize(executor));
		assertEquals(Integer.MAX_VALUE, getQueueCapacity(executor));
		assertEquals(60, getKeepAliveSeconds(executor));
		assertEquals(false, getAllowCoreThreadTimeOut(executor));
		FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "foo";
			}
		});
		((ThreadPoolTaskExecutor)executor).execute(task);
		assertEquals("foo", task.get());
	}

	@Test
	public void singleSize() {
		Object executor = this.context.getBean("singleSize");
		assertEquals(42, getCorePoolSize(executor));
		assertEquals(42, getMaxPoolSize(executor));
	}

	@Test(expected = BeanCreationException.class)
	public void invalidPoolSize() {
		this.context.getBean("invalidPoolSize");
	}

	@Test
	public void rangeWithBoundedQueue() {
		Object executor = this.context.getBean("rangeWithBoundedQueue");
		assertEquals(7, getCorePoolSize(executor));
		assertEquals(42, getMaxPoolSize(executor));
		assertEquals(11, getQueueCapacity(executor));
	}

	@Test
	public void rangeWithUnboundedQueue() {
		Object executor = this.context.getBean("rangeWithUnboundedQueue");
		assertEquals(9, getCorePoolSize(executor));
		assertEquals(9, getMaxPoolSize(executor));
		assertEquals(37, getKeepAliveSeconds(executor));
		assertEquals(true, getAllowCoreThreadTimeOut(executor));
		assertEquals(Integer.MAX_VALUE, getQueueCapacity(executor));
	}

	@Test
	public void propertyPlaceholderWithSingleSize() {
		Object executor = this.context.getBean("propertyPlaceholderWithSingleSize");
		assertEquals(123, getCorePoolSize(executor));
		assertEquals(123, getMaxPoolSize(executor));
		assertEquals(60, getKeepAliveSeconds(executor));
		assertEquals(false, getAllowCoreThreadTimeOut(executor));
		assertEquals(Integer.MAX_VALUE, getQueueCapacity(executor));
	}

	@Test
	public void propertyPlaceholderWithRange() {
		Object executor = this.context.getBean("propertyPlaceholderWithRange");
		assertEquals(5, getCorePoolSize(executor));
		assertEquals(25, getMaxPoolSize(executor));
		assertEquals(false, getAllowCoreThreadTimeOut(executor));
		assertEquals(10, getQueueCapacity(executor));
	}

	@Test
	public void propertyPlaceholderWithRangeAndCoreThreadTimeout() {
		Object executor = this.context.getBean("propertyPlaceholderWithRangeAndCoreThreadTimeout");
		assertEquals(99, getCorePoolSize(executor));
		assertEquals(99, getMaxPoolSize(executor));
		assertEquals(true, getAllowCoreThreadTimeOut(executor));
	}

	@Test(expected = BeanCreationException.class)
	public void propertyPlaceholderWithInvalidPoolSize() {
		this.context.getBean("propertyPlaceholderWithInvalidPoolSize");
	}

	@Test
	public void threadNamePrefix() {
		CustomizableThreadCreator executor = this.context.getBean("default", CustomizableThreadCreator.class);
		assertEquals("default-", executor.getThreadNamePrefix());
	}

	@Test
	public void typeCheck() {
		assertTrue(this.context.isTypeMatch("default", Executor.class));
		assertTrue(this.context.isTypeMatch("default", TaskExecutor.class));
		assertTrue(this.context.isTypeMatch("default", ThreadPoolTaskExecutor.class));
	}


	private int getCorePoolSize(Object executor) {
		return (Integer) new DirectFieldAccessor(executor).getPropertyValue("corePoolSize");
	}

	private int getMaxPoolSize(Object executor) {
		return (Integer) new DirectFieldAccessor(executor).getPropertyValue("maxPoolSize");
	}

	private int getQueueCapacity(Object executor) {
		return (Integer) new DirectFieldAccessor(executor).getPropertyValue("queueCapacity");
	}

	private int getKeepAliveSeconds(Object executor) {
		return (Integer) new DirectFieldAccessor(executor).getPropertyValue("keepAliveSeconds");
	}

	private boolean getAllowCoreThreadTimeOut(Object executor) {
		return (Boolean) new DirectFieldAccessor(executor).getPropertyValue("allowCoreThreadTimeOut");
	}

}
