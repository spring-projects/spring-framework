/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.scheduling.config;

import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Stephane Nicoll
 */
public class AnnotationDrivenBeanDefinitionParserTests {

	private ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
			"annotationDrivenContext.xml", AnnotationDrivenBeanDefinitionParserTests.class);


	@AfterEach
	public void closeApplicationContext() {
		context.close();
	}


	@Test
	public void asyncPostProcessorRegistered() {
		assertThat(context.containsBean(TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
	}

	@Test
	public void scheduledPostProcessorRegistered() {
		assertThat(context.containsBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
	}

	@Test
	public void asyncPostProcessorExecutorReference() {
		Object executor = context.getBean("testExecutor");
		Object postProcessor = context.getBean(TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME);
		assertThat(((Supplier<?>) new DirectFieldAccessor(postProcessor).getPropertyValue("executor")).get()).isSameAs(executor);
	}

	@Test
	public void scheduledPostProcessorSchedulerReference() {
		Object scheduler = context.getBean("testScheduler");
		Object postProcessor = context.getBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
		assertThat(new DirectFieldAccessor(postProcessor).getPropertyValue("scheduler")).isSameAs(scheduler);
	}

	@Test
	public void asyncPostProcessorExceptionHandlerReference() {
		Object exceptionHandler = context.getBean("testExceptionHandler");
		Object postProcessor = context.getBean(TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME);
		assertThat(((Supplier<?>) new DirectFieldAccessor(postProcessor).getPropertyValue("exceptionHandler")).get()).isSameAs(exceptionHandler);
	}

}
