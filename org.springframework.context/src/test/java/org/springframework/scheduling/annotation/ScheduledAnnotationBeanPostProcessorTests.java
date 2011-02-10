/*
 * Copyright 2002-2011 the original author or authors.
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

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
public class ScheduledAnnotationBeanPostProcessorTests {

	@Test
	public void fixedDelayTask() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(
				ScheduledAnnotationBeanPostProcessorTests.FixedDelayTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
		Object postProcessor = context.getBean("postProcessor");
		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		Map<Runnable, Long> fixedDelayTasks = (Map<Runnable, Long>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedDelayTasks");
		assertEquals(1, fixedDelayTasks.size());
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) fixedDelayTasks.keySet().iterator().next();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("fixedDelay", targetMethod.getName());
		assertEquals(new Long(5000), fixedDelayTasks.values().iterator().next());
	}

	@Test
	public void fixedRateTask() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(
				ScheduledAnnotationBeanPostProcessorTests.FixedRateTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
		Object postProcessor = context.getBean("postProcessor");
		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		Map<Runnable, Long> fixedRateTasks = (Map<Runnable, Long>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertEquals(1, fixedRateTasks.size());
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) fixedRateTasks.keySet().iterator().next();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("fixedRate", targetMethod.getName());
		assertEquals(new Long(3000), fixedRateTasks.values().iterator().next());
	}

	@Test
	public void cronTask() throws InterruptedException {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(
				ScheduledAnnotationBeanPostProcessorTests.CronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
		Object postProcessor = context.getBean("postProcessor");
		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		Map<Runnable, String> cronTasks = (Map<Runnable, String>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertEquals(1, cronTasks.size());
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) cronTasks.keySet().iterator().next();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("cron", targetMethod.getName());
		assertEquals("*/7 * * * * ?", cronTasks.values().iterator().next());
		Thread.sleep(10000);
	}

	@Test
	public void metaAnnotationWithFixedRate() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(
				ScheduledAnnotationBeanPostProcessorTests.MetaAnnotationFixedRateTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
		Object postProcessor = context.getBean("postProcessor");
		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		Map<Runnable, Long> fixedRateTasks = (Map<Runnable, Long>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertEquals(1, fixedRateTasks.size());
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) fixedRateTasks.keySet().iterator().next();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("checkForUpdates", targetMethod.getName());
		assertEquals(new Long(5000), fixedRateTasks.values().iterator().next());
	}

	@Test
	public void metaAnnotationWithCronExpression() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(
				ScheduledAnnotationBeanPostProcessorTests.MetaAnnotationCronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
		Object postProcessor = context.getBean("postProcessor");
		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		Map<Runnable, String> cronTasks = (Map<Runnable, String>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertEquals(1, cronTasks.size());
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) cronTasks.keySet().iterator().next();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("generateReport", targetMethod.getName());
		assertEquals("0 0 * * * ?", cronTasks.values().iterator().next());
	}

	@Test
	public void propertyPlaceholderWithCronExpression() {
		String businessHoursCronExpression = "0 0 9-17 * * MON-FRI";
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertyPlaceholderConfigurer.class);
		Properties properties = new Properties();
		properties.setProperty("schedules.businessHours", businessHoursCronExpression);
		placeholderDefinition.getPropertyValues().addPropertyValue("properties", properties);
		BeanDefinition targetDefinition = new RootBeanDefinition(
				ScheduledAnnotationBeanPostProcessorTests.PropertyPlaceholderTestBean.class);
		context.registerBeanDefinition("placeholder", placeholderDefinition);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
		Object postProcessor = context.getBean("postProcessor");
		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		Map<Runnable, String> cronTasks = (Map<Runnable, String>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertEquals(1, cronTasks.size());
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) cronTasks.keySet().iterator().next();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("x", targetMethod.getName());
		assertEquals(businessHoursCronExpression, cronTasks.values().iterator().next());
	}

	@Test
	public void propertyPlaceholderForMetaAnnotation() {
		String businessHoursCronExpression = "0 0 9-17 * * MON-FRI";
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertyPlaceholderConfigurer.class);
		Properties properties = new Properties();
		properties.setProperty("schedules.businessHours", businessHoursCronExpression);
		placeholderDefinition.getPropertyValues().addPropertyValue("properties", properties);
		BeanDefinition targetDefinition = new RootBeanDefinition(
				ScheduledAnnotationBeanPostProcessorTests.PropertyPlaceholderMetaAnnotationTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("placeholder", placeholderDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
		Object postProcessor = context.getBean("postProcessor");
		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		Map<Runnable, String> cronTasks = (Map<Runnable, String>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertEquals(1, cronTasks.size());
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) cronTasks.keySet().iterator().next();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("y", targetMethod.getName());
		assertEquals(businessHoursCronExpression, cronTasks.values().iterator().next());
	}

	@Test(expected = BeanCreationException.class)
	public void emptyAnnotation() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(
				ScheduledAnnotationBeanPostProcessorTests.EmptyAnnotationTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidCron() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(
				ScheduledAnnotationBeanPostProcessorTests.InvalidCronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
	}

	@Test(expected = BeanCreationException.class)
	public void nonVoidReturnType() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(
				ScheduledAnnotationBeanPostProcessorTests.NonVoidReturnTypeTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
	}

	@Test(expected = BeanCreationException.class)
	public void nonEmptyParamList() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(
				ScheduledAnnotationBeanPostProcessorTests.NonEmptyParamListTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
	}


	public static class FixedDelayTestBean {

		@Scheduled(fixedDelay=5000)
		public void fixedDelay() {
		}
	}


	public static class FixedRateTestBean {

		@Scheduled(fixedRate=3000)
		public void fixedRate() {
		}
	}


	public static class CronTestBean {

		@Scheduled(cron="*/7 * * * * ?")
		public void cron() throws IOException {
			throw new IOException("no no no");
		}

	}


	private static class EmptyAnnotationTestBean {

		@Scheduled
		public void invalid() {
		}

	}


	private static class InvalidCronTestBean {

		@Scheduled(cron="abc")
		public void invalid() {
		}

	}


	private static class NonVoidReturnTypeTestBean {

		@Scheduled(fixedRate=3000)
		public String invalid() {
			return "oops";
		}

	}


	private static class NonEmptyParamListTestBean {

		@Scheduled(fixedRate=3000)
		public void invalid(String oops) {
		}

	}


	@Scheduled(fixedRate = 5000)
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface EveryFiveSeconds {}


	@Scheduled(cron = "0 0 * * * ?")
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface Hourly {}


	private static class MetaAnnotationFixedRateTestBean {

		@EveryFiveSeconds
		public void checkForUpdates() {
		}
	}


	private static class MetaAnnotationCronTestBean {

		@Hourly
		public void generateReport() {
		}
	}


	private static class PropertyPlaceholderTestBean {

		@Scheduled(cron = "${schedules.businessHours}")
		public void x() {
		}
	}


	@Scheduled(cron = "${schedules.businessHours}")
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)	
	private static @interface BusinessHours {}


	private static class PropertyPlaceholderMetaAnnotationTestBean {

		@BusinessHours
		public void y() {
		}
	}

}
