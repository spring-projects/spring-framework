/*
 * Copyright 2002-2009 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Mark Fisher
 */
public class ExecutorBeanDefinitionParserTests {

	private ApplicationContext context;


	@Before
	public void setup() {
		this.context = new ClassPathXmlApplicationContext(
				"executorContext.xml", ExecutorBeanDefinitionParserTests.class);
	}

	@Test
	public void defaultExecutor() {
		Object executor = this.context.getBean("default");
		assertEquals(1, this.getCorePoolSize(executor));
		assertEquals(Integer.MAX_VALUE, this.getMaxPoolSize(executor));
		assertEquals(Integer.MAX_VALUE, this.getQueueCapacity(executor));
		assertEquals(60, this.getKeepAliveSeconds(executor));
		assertEquals(false, this.getAllowCoreThreadTimeOut(executor));
	}

	@Test
	public void singleSize() {
		Object executor = this.context.getBean("singleSize");
		assertEquals(42, this.getCorePoolSize(executor));
		assertEquals(42, this.getMaxPoolSize(executor));
	}

	@Test
	public void rangeWithBoundedQueue() {
		Object executor = this.context.getBean("rangeWithBoundedQueue");
		assertEquals(7, this.getCorePoolSize(executor));
		assertEquals(42, this.getMaxPoolSize(executor));
		assertEquals(11, this.getQueueCapacity(executor));
	}

	@Test
	public void rangeWithUnboundedQueue() {
		Object executor = this.context.getBean("rangeWithUnboundedQueue");
		assertEquals(9, this.getCorePoolSize(executor));
		assertEquals(9, this.getMaxPoolSize(executor));
		assertEquals(37, this.getKeepAliveSeconds(executor));
		assertEquals(true, this.getAllowCoreThreadTimeOut(executor));
		assertEquals(Integer.MAX_VALUE, this.getQueueCapacity(executor));
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
