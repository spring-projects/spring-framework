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
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.support.MethodInvokingRunnable;

/**
 * @author Mark Fisher
 */
@SuppressWarnings("unchecked")
public class ScheduledTasksBeanDefinitionParserTests {

	private ApplicationContext context;

	private ScheduledTaskRegistrar registrar;

	private Object testBean;


	@Before
	public void setup() {
		this.context = new ClassPathXmlApplicationContext(
				"scheduledTasksContext.xml", ScheduledTasksBeanDefinitionParserTests.class);
		this.registrar = (ScheduledTaskRegistrar) this.context.getBeansOfType(
				ScheduledTaskRegistrar.class).values().iterator().next();
		this.testBean = this.context.getBean("testBean");
	}

	@Test
	public void checkScheduler() {
		Object schedulerBean = this.context.getBean("testScheduler");
		Object schedulerRef = new DirectFieldAccessor(this.registrar).getPropertyValue("taskScheduler");
		assertEquals(schedulerBean, schedulerRef);
	}

	@Test
	public void checkTarget() {
		Map<Runnable, Long> tasks = (Map<Runnable, Long>) new DirectFieldAccessor(
				this.registrar).getPropertyValue("fixedRateTasks");
		Runnable runnable = tasks.keySet().iterator().next();
		assertEquals(MethodInvokingRunnable.class, runnable.getClass());
		Object targetObject = ((MethodInvokingRunnable) runnable).getTargetObject();
		String targetMethod = ((MethodInvokingRunnable) runnable).getTargetMethod();
		assertEquals(this.testBean, targetObject);
		assertEquals("test", targetMethod);
	}

	@Test
	public void fixedRateTasks() {
		Map<Runnable, Long> tasks = (Map<Runnable, Long>) new DirectFieldAccessor(
				this.registrar).getPropertyValue("fixedRateTasks");
		assertEquals(2, tasks.size());
		Collection<Long> values = tasks.values();
		assertTrue(values.contains(new Long(1000)));
		assertTrue(values.contains(new Long(2000)));
	}

	@Test
	public void fixedDelayTasks() {
		Map<Runnable, Long> tasks = (Map<Runnable, Long>) new DirectFieldAccessor(
				this.registrar).getPropertyValue("fixedDelayTasks");
		assertEquals(1, tasks.size());
		Long value = tasks.values().iterator().next();
		assertEquals(new Long(3000), value);
	}

	@Test
	public void cronTasks() {
		Map<Runnable, String> tasks = (Map<Runnable, String>) new DirectFieldAccessor(
				this.registrar).getPropertyValue("cronTasks");
		assertEquals(1, tasks.size());
		String expression = tasks.values().iterator().next();
		assertEquals("*/4 * 9-17 * * MON-FRI", expression);		
	}


	static class TestBean {

		public void test() {
		}

	}

}
