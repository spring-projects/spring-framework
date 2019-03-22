/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.scheduling.aspectj;

import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class AnnotationDrivenBeanDefinitionParserTests {

	private ConfigurableApplicationContext context;

	@Before
	public void setup() {
		this.context = new ClassPathXmlApplicationContext(
				"annotationDrivenContext.xml", AnnotationDrivenBeanDefinitionParserTests.class);
	}

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void asyncAspectRegistered() {
		assertTrue(context.containsBean(TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void asyncPostProcessorExecutorReference() {
		Object executor = context.getBean("testExecutor");
		Object aspect = context.getBean(TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME);
		assertSame(executor, ((Supplier) new DirectFieldAccessor(aspect).getPropertyValue("defaultExecutor")).get());
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void asyncPostProcessorExceptionHandlerReference() {
		Object exceptionHandler = context.getBean("testExceptionHandler");
		Object aspect = context.getBean(TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME);
		assertSame(exceptionHandler, ((Supplier) new DirectFieldAccessor(aspect).getPropertyValue("exceptionHandler")).get());
	}

}
