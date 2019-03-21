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

package org.springframework.scheduling.annotation;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.annotation.AliasFor;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Stevo Slavić
 */
public class ScheduledAnnotationBeanPostProcessorTests {

	private final StaticApplicationContext context = new StaticApplicationContext();


	@After
	public void closeContextAfterTest() {
		context.close();
	}


	@Test
	public void fixedDelayTask() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(FixedDelayTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedDelayTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedDelayTasks");
		assertEquals(1, fixedDelayTasks.size());
		IntervalTask task = fixedDelayTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("fixedDelay", targetMethod.getName());
		assertEquals(0L, task.getInitialDelay());
		assertEquals(5000L, task.getInterval());
	}

	@Test
	public void fixedRateTask() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(FixedRateTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertEquals(1, fixedRateTasks.size());
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("fixedRate", targetMethod.getName());
		assertEquals(0L, task.getInitialDelay());
		assertEquals(3000L, task.getInterval());
	}

	@Test
	public void fixedRateTaskWithInitialDelay() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(FixedRateWithInitialDelayTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertEquals(1, fixedRateTasks.size());
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("fixedRate", targetMethod.getName());
		assertEquals(1000L, task.getInitialDelay());
		assertEquals(3000L, task.getInterval());
	}

	@Test
	public void severalFixedRatesWithRepeatedScheduledAnnotation() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(SeveralFixedRatesWithRepeatedScheduledAnnotationTestBean.class);
		severalFixedRates(context, processorDefinition, targetDefinition);
	}

	@Test
	public void severalFixedRatesWithSchedulesContainerAnnotation() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(SeveralFixedRatesWithSchedulesContainerAnnotationTestBean.class);
		severalFixedRates(context, processorDefinition, targetDefinition);
	}

	@Test
	public void severalFixedRatesOnBaseClass() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(FixedRatesSubBean.class);
		severalFixedRates(context, processorDefinition, targetDefinition);
	}

	@Test
	public void severalFixedRatesOnDefaultMethod() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(FixedRatesDefaultBean.class);
		severalFixedRates(context, processorDefinition, targetDefinition);
	}

	@Test
	public void severalFixedRatesAgainstNestedCglibProxy() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(SeveralFixedRatesWithRepeatedScheduledAnnotationTestBean.class);
		targetDefinition.setFactoryMethodName("nestedProxy");
		severalFixedRates(context, processorDefinition, targetDefinition);
	}

	private void severalFixedRates(StaticApplicationContext context,
			BeanDefinition processorDefinition, BeanDefinition targetDefinition) {

		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(2, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertEquals(2, fixedRateTasks.size());
		IntervalTask task1 = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable1 = (ScheduledMethodRunnable) task1.getRunnable();
		Object targetObject = runnable1.getTarget();
		Method targetMethod = runnable1.getMethod();
		assertEquals(target, targetObject);
		assertEquals("fixedRate", targetMethod.getName());
		assertEquals(0, task1.getInitialDelay());
		assertEquals(4000L, task1.getInterval());
		IntervalTask task2 = fixedRateTasks.get(1);
		ScheduledMethodRunnable runnable2 = (ScheduledMethodRunnable) task2.getRunnable();
		targetObject = runnable2.getTarget();
		targetMethod = runnable2.getMethod();
		assertEquals(target, targetObject);
		assertEquals("fixedRate", targetMethod.getName());
		assertEquals(2000L, task2.getInitialDelay());
		assertEquals(4000L, task2.getInterval());
	}

	@Test
	public void cronTask() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(CronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertEquals(1, cronTasks.size());
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("cron", targetMethod.getName());
		assertEquals("*/7 * * * * ?", task.getExpression());
	}

	@Test
	public void cronTaskWithZone() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(CronWithTimezoneTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertEquals(1, cronTasks.size());
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("cron", targetMethod.getName());
		assertEquals("0 0 0-4,6-23 * * ?", task.getExpression());
		Trigger trigger = task.getTrigger();
		assertNotNull(trigger);
		assertTrue(trigger instanceof CronTrigger);
		CronTrigger cronTrigger = (CronTrigger) trigger;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+10"));
		cal.clear();
		cal.set(2013, 3, 15, 4, 0);  // 15-04-2013 4:00 GMT+10
		Date lastScheduledExecutionTime = cal.getTime();
		Date lastActualExecutionTime = cal.getTime();
		cal.add(Calendar.MINUTE, 30);  // 4:30
		Date lastCompletionTime = cal.getTime();
		TriggerContext triggerContext = new SimpleTriggerContext(
				lastScheduledExecutionTime, lastActualExecutionTime, lastCompletionTime);
		cal.add(Calendar.MINUTE, 30);
		cal.add(Calendar.HOUR_OF_DAY, 1);  // 6:00
		Date nextExecutionTime = cronTrigger.nextExecutionTime(triggerContext);
		assertEquals(cal.getTime(), nextExecutionTime);  // assert that 6:00 is next execution time
	}

	@Test(expected = BeanCreationException.class)
	public void cronTaskWithInvalidZone() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(CronWithInvalidTimezoneTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
	}

	@Test(expected = BeanCreationException.class)
	public void cronTaskWithMethodValidation() {
		BeanDefinition validationDefinition = new RootBeanDefinition(MethodValidationPostProcessor.class);
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(CronTestBean.class);
		context.registerBeanDefinition("methodValidation", validationDefinition);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
	}

	@Test
	public void cronTaskWithScopedProxy() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		new AnnotatedBeanDefinitionReader(context).register(ProxiedCronTestBean.class, ProxiedCronTestBeanDependent.class);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertEquals(1, cronTasks.size());
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(context.getBean(ScopedProxyUtils.getTargetBeanName("target")), targetObject);
		assertEquals("cron", targetMethod.getName());
		assertEquals("*/7 * * * * ?", task.getExpression());
	}

	@Test
	public void metaAnnotationWithFixedRate() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(MetaAnnotationFixedRateTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertEquals(1, fixedRateTasks.size());
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("checkForUpdates", targetMethod.getName());
		assertEquals(5000L, task.getInterval());
	}

	@Test
	public void composedAnnotationWithInitialDelayAndFixedRate() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(ComposedAnnotationFixedRateTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertEquals(1, fixedRateTasks.size());
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("checkForUpdates", targetMethod.getName());
		assertEquals(5000L, task.getInterval());
		assertEquals(1000L, task.getInitialDelay());
	}

	@Test
	public void metaAnnotationWithCronExpression() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(MetaAnnotationCronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertEquals(1, cronTasks.size());
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("generateReport", targetMethod.getName());
		assertEquals("0 0 * * * ?", task.getExpression());
	}

	@Test
	public void propertyPlaceholderWithCron() {
		String businessHoursCronExpression = "0 0 9-17 * * MON-FRI";
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertyPlaceholderConfigurer.class);
		Properties properties = new Properties();
		properties.setProperty("schedules.businessHours", businessHoursCronExpression);
		placeholderDefinition.getPropertyValues().addPropertyValue("properties", properties);
		BeanDefinition targetDefinition = new RootBeanDefinition(PropertyPlaceholderWithCronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("placeholder", placeholderDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertEquals(1, cronTasks.size());
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("x", targetMethod.getName());
		assertEquals(businessHoursCronExpression, task.getExpression());
	}

	@Test
	public void propertyPlaceholderWithFixedDelayInMillis() {
		propertyPlaceholderWithFixedDelay(false);
	}

	@Test
	public void propertyPlaceholderWithFixedDelayInDuration() {
		propertyPlaceholderWithFixedDelay(true);
	}

	private void propertyPlaceholderWithFixedDelay(boolean durationFormat) {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertyPlaceholderConfigurer.class);
		Properties properties = new Properties();
		properties.setProperty("fixedDelay", (durationFormat ? "PT5S" : "5000"));
		properties.setProperty("initialDelay", (durationFormat ? "PT1S" : "1000"));
		placeholderDefinition.getPropertyValues().addPropertyValue("properties", properties);
		BeanDefinition targetDefinition = new RootBeanDefinition(PropertyPlaceholderWithFixedDelayTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("placeholder", placeholderDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedDelayTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedDelayTasks");
		assertEquals(1, fixedDelayTasks.size());
		IntervalTask task = fixedDelayTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("fixedDelay", targetMethod.getName());
		assertEquals(1000L, task.getInitialDelay());
		assertEquals(5000L, task.getInterval());
	}

	@Test
	public void propertyPlaceholderWithFixedRateInMillis() {
		propertyPlaceholderWithFixedRate(false);
	}

	@Test
	public void propertyPlaceholderWithFixedRateInDuration() {
		propertyPlaceholderWithFixedRate(true);
	}

	private void propertyPlaceholderWithFixedRate(boolean durationFormat) {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertyPlaceholderConfigurer.class);
		Properties properties = new Properties();
		properties.setProperty("fixedRate", (durationFormat ? "PT3S" : "3000"));
		properties.setProperty("initialDelay", (durationFormat ? "PT1S" : "1000"));
		placeholderDefinition.getPropertyValues().addPropertyValue("properties", properties);
		BeanDefinition targetDefinition = new RootBeanDefinition(PropertyPlaceholderWithFixedRateTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("placeholder", placeholderDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertEquals(1, fixedRateTasks.size());
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("fixedRate", targetMethod.getName());
		assertEquals(1000L, task.getInitialDelay());
		assertEquals(3000L, task.getInterval());
	}

	@Test
	public void expressionWithCron() {
		String businessHoursCronExpression = "0 0 9-17 * * MON-FRI";
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(ExpressionWithCronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		Map<String, String> schedules = new HashMap<>();
		schedules.put("businessHours", businessHoursCronExpression);
		context.getBeanFactory().registerSingleton("schedules", schedules);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertEquals(1, cronTasks.size());
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("x", targetMethod.getName());
		assertEquals(businessHoursCronExpression, task.getExpression());
	}

	@Test
	public void propertyPlaceholderForMetaAnnotation() {
		String businessHoursCronExpression = "0 0 9-17 * * MON-FRI";
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertyPlaceholderConfigurer.class);
		Properties properties = new Properties();
		properties.setProperty("schedules.businessHours", businessHoursCronExpression);
		placeholderDefinition.getPropertyValues().addPropertyValue("properties", properties);
		BeanDefinition targetDefinition = new RootBeanDefinition(PropertyPlaceholderMetaAnnotationTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("placeholder", placeholderDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertEquals(1, cronTasks.size());
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("y", targetMethod.getName());
		assertEquals(businessHoursCronExpression, task.getExpression());
	}

	@Test
	public void nonVoidReturnType() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(NonVoidReturnTypeTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertEquals(1, postProcessor.getScheduledTasks().size());

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertEquals(1, cronTasks.size());
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertEquals(target, targetObject);
		assertEquals("cron", targetMethod.getName());
		assertEquals("0 0 9-17 * * MON-FRI", task.getExpression());
	}

	@Test(expected = BeanCreationException.class)
	public void emptyAnnotation() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(EmptyAnnotationTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
	}

	@Test(expected = BeanCreationException.class)
	public void invalidCron() throws Throwable {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(InvalidCronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
	}

	@Test(expected = BeanCreationException.class)
	public void nonEmptyParamList() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(NonEmptyParamListTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();
	}


	static class FixedDelayTestBean {

		@Scheduled(fixedDelay = 5000)
		public void fixedDelay() {
		}
	}


	static class FixedRateTestBean {

		@Scheduled(fixedRate = 3000)
		public void fixedRate() {
		}
	}


	static class FixedRateWithInitialDelayTestBean {

		@Scheduled(fixedRate = 3000, initialDelay = 1000)
		public void fixedRate() {
		}
	}


	static class SeveralFixedRatesWithSchedulesContainerAnnotationTestBean {

		@Schedules({@Scheduled(fixedRate = 4000), @Scheduled(fixedRate = 4000, initialDelay = 2000)})
		public void fixedRate() {
		}
	}


	static class SeveralFixedRatesWithRepeatedScheduledAnnotationTestBean {

		@Scheduled(fixedRate = 4000)
		@Scheduled(fixedRate = 4000, initialDelay = 2000)
		public void fixedRate() {
		}

		static SeveralFixedRatesWithRepeatedScheduledAnnotationTestBean nestedProxy() {
			ProxyFactory pf1 = new ProxyFactory(new SeveralFixedRatesWithRepeatedScheduledAnnotationTestBean());
			pf1.setProxyTargetClass(true);
			ProxyFactory pf2 = new ProxyFactory(pf1.getProxy());
			pf2.setProxyTargetClass(true);
			return (SeveralFixedRatesWithRepeatedScheduledAnnotationTestBean) pf2.getProxy();
		}
	}


	static class FixedRatesBaseBean {

		@Scheduled(fixedRate = 4000)
		@Scheduled(fixedRate = 4000, initialDelay = 2000)
		public void fixedRate() {
		}
	}


	static class FixedRatesSubBean extends FixedRatesBaseBean {
	}


	interface FixedRatesDefaultMethod {

		@Scheduled(fixedRate = 4000)
		@Scheduled(fixedRate = 4000, initialDelay = 2000)
		default void fixedRate() {
		}
	}


	static class FixedRatesDefaultBean implements FixedRatesDefaultMethod {
	}


	@Validated
	static class CronTestBean {

		@Scheduled(cron = "*/7 * * * * ?")
		private void cron() throws IOException {
			throw new IOException("no no no");
		}
	}


	static class CronWithTimezoneTestBean {

		@Scheduled(cron = "0 0 0-4,6-23 * * ?", zone = "GMT+10")
		protected void cron() throws IOException {
			throw new IOException("no no no");
		}
	}


	static class CronWithInvalidTimezoneTestBean {

		@Scheduled(cron = "0 0 0-4,6-23 * * ?", zone = "FOO")
		public void cron() throws IOException {
			throw new IOException("no no no");
		}
	}


	@Component("target")
	@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
	static class ProxiedCronTestBean {

		@Scheduled(cron = "*/7 * * * * ?")
		public void cron() throws IOException {
			throw new IOException("no no no");
		}
	}


	static class ProxiedCronTestBeanDependent {

		public ProxiedCronTestBeanDependent(ProxiedCronTestBean testBean) {
		}
	}


	static class NonVoidReturnTypeTestBean {

		@Scheduled(cron = "0 0 9-17 * * MON-FRI")
		public String cron() {
			return "oops";
		}
	}


	static class EmptyAnnotationTestBean {

		@Scheduled
		public void invalid() {
		}
	}


	static class InvalidCronTestBean {

		@Scheduled(cron = "abc")
		public void invalid() {
		}
	}


	static class NonEmptyParamListTestBean {

		@Scheduled(fixedRate = 3000)
		public void invalid(String oops) {
		}
	}


	@Scheduled(fixedRate = 5000)
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface EveryFiveSeconds {
	}

	@Scheduled(cron = "0 0 * * * ?")
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface Hourly {
	}

	@Scheduled(initialDelay = 1000)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface WaitASec {

		@AliasFor(annotation = Scheduled.class)
		long fixedDelay() default -1;

		@AliasFor(annotation = Scheduled.class)
		long fixedRate() default -1;
	}


	static class MetaAnnotationFixedRateTestBean {

		@EveryFiveSeconds
		public void checkForUpdates() {
		}
	}


	static class ComposedAnnotationFixedRateTestBean {

		@WaitASec(fixedRate = 5000)
		public void checkForUpdates() {
		}
	}


	static class MetaAnnotationCronTestBean {

		@Hourly
		public void generateReport() {
		}
	}


	static class PropertyPlaceholderWithCronTestBean {

		@Scheduled(cron = "${schedules.businessHours}")
		public void x() {
		}
	}


	static class PropertyPlaceholderWithFixedDelayTestBean {

		@Scheduled(fixedDelayString = "${fixedDelay}", initialDelayString = "${initialDelay}")
		public void fixedDelay() {
		}
	}


	static class PropertyPlaceholderWithFixedRateTestBean {

		@Scheduled(fixedRateString = "${fixedRate}", initialDelayString = "${initialDelay}")
		public void fixedRate() {
		}
	}


	static class ExpressionWithCronTestBean {

		@Scheduled(cron = "#{schedules.businessHours}")
		public void x() {
		}
	}


	@Scheduled(cron = "${schedules.businessHours}")
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface BusinessHours {
	}


	static class PropertyPlaceholderMetaAnnotationTestBean {

		@BusinessHours
		public void y() {
		}
	}

}
