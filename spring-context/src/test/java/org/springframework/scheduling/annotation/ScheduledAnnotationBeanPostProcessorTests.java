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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Stevo Slavić
 */
public class ScheduledAnnotationBeanPostProcessorTests {

	private final StaticApplicationContext context = new StaticApplicationContext();


	@AfterEach
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
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedDelayTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedDelayTasks");
		assertThat(fixedDelayTasks.size()).isEqualTo(1);
		IntervalTask task = fixedDelayTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedDelay");
		assertThat(task.getInitialDelay()).isEqualTo(0L);
		assertThat(task.getInterval()).isEqualTo(5000L);
	}

	@Test
	public void fixedRateTask() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(FixedRateTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertThat(fixedRateTasks.size()).isEqualTo(1);
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedRate");
		assertThat(task.getInitialDelay()).isEqualTo(0L);
		assertThat(task.getInterval()).isEqualTo(3000L);
	}

	@Test
	public void fixedRateTaskWithInitialDelay() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(FixedRateWithInitialDelayTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertThat(fixedRateTasks.size()).isEqualTo(1);
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedRate");
		assertThat(task.getInitialDelay()).isEqualTo(1000L);
		assertThat(task.getInterval()).isEqualTo(3000L);
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
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(2);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertThat(fixedRateTasks.size()).isEqualTo(2);
		IntervalTask task1 = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable1 = (ScheduledMethodRunnable) task1.getRunnable();
		Object targetObject = runnable1.getTarget();
		Method targetMethod = runnable1.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedRate");
		assertThat(task1.getInitialDelay()).isEqualTo(0);
		assertThat(task1.getInterval()).isEqualTo(4000L);
		IntervalTask task2 = fixedRateTasks.get(1);
		ScheduledMethodRunnable runnable2 = (ScheduledMethodRunnable) task2.getRunnable();
		targetObject = runnable2.getTarget();
		targetMethod = runnable2.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedRate");
		assertThat(task2.getInitialDelay()).isEqualTo(2000L);
		assertThat(task2.getInterval()).isEqualTo(4000L);
	}

	@Test
	public void cronTask() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(CronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks.size()).isEqualTo(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("cron");
		assertThat(task.getExpression()).isEqualTo("*/7 * * * * ?");
	}

	@Test
	public void cronTaskWithZone() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(CronWithTimezoneTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks.size()).isEqualTo(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("cron");
		assertThat(task.getExpression()).isEqualTo("0 0 0-4,6-23 * * ?");
		Trigger trigger = task.getTrigger();
		assertThat(trigger).isNotNull();
		boolean condition = trigger instanceof CronTrigger;
		assertThat(condition).isTrue();
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
		// assert that 6:00 is next execution time
		assertThat(nextExecutionTime).isEqualTo(cal.getTime());
	}

	@Test
	public void cronTaskWithInvalidZone() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(CronWithInvalidTimezoneTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				context::refresh);
	}

	@Test
	public void cronTaskWithMethodValidation() {
		BeanDefinition validationDefinition = new RootBeanDefinition(MethodValidationPostProcessor.class);
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(CronTestBean.class);
		context.registerBeanDefinition("methodValidation", validationDefinition);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				context::refresh);
	}

	@Test
	public void cronTaskWithScopedProxy() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		new AnnotatedBeanDefinitionReader(context).register(ProxiedCronTestBean.class, ProxiedCronTestBeanDependent.class);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks.size()).isEqualTo(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(context.getBean(ScopedProxyUtils.getTargetBeanName("target")));
		assertThat(targetMethod.getName()).isEqualTo("cron");
		assertThat(task.getExpression()).isEqualTo("*/7 * * * * ?");
	}

	@Test
	public void metaAnnotationWithFixedRate() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(MetaAnnotationFixedRateTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertThat(fixedRateTasks.size()).isEqualTo(1);
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("checkForUpdates");
		assertThat(task.getInterval()).isEqualTo(5000L);
	}

	@Test
	public void composedAnnotationWithInitialDelayAndFixedRate() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(ComposedAnnotationFixedRateTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertThat(fixedRateTasks.size()).isEqualTo(1);
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("checkForUpdates");
		assertThat(task.getInterval()).isEqualTo(5000L);
		assertThat(task.getInitialDelay()).isEqualTo(1000L);
	}

	@Test
	public void metaAnnotationWithCronExpression() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(MetaAnnotationCronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks.size()).isEqualTo(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("generateReport");
		assertThat(task.getExpression()).isEqualTo("0 0 * * * ?");
	}

	@Test
	public void propertyPlaceholderWithCron() {
		String businessHoursCronExpression = "0 0 9-17 * * MON-FRI";
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertySourcesPlaceholderConfigurer.class);
		Properties properties = new Properties();
		properties.setProperty("schedules.businessHours", businessHoursCronExpression);
		placeholderDefinition.getPropertyValues().addPropertyValue("properties", properties);
		BeanDefinition targetDefinition = new RootBeanDefinition(PropertyPlaceholderWithCronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("placeholder", placeholderDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks.size()).isEqualTo(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("x");
		assertThat(task.getExpression()).isEqualTo(businessHoursCronExpression);
	}

	@Test
	public void propertyPlaceholderWithInactiveCron() {
		String businessHoursCronExpression = "-";
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertySourcesPlaceholderConfigurer.class);
		Properties properties = new Properties();
		properties.setProperty("schedules.businessHours", businessHoursCronExpression);
		placeholderDefinition.getPropertyValues().addPropertyValue("properties", properties);
		BeanDefinition targetDefinition = new RootBeanDefinition(PropertyPlaceholderWithCronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("placeholder", placeholderDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks().isEmpty()).isTrue();
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
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertySourcesPlaceholderConfigurer.class);
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
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedDelayTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedDelayTasks");
		assertThat(fixedDelayTasks.size()).isEqualTo(1);
		IntervalTask task = fixedDelayTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedDelay");
		assertThat(task.getInitialDelay()).isEqualTo(1000L);
		assertThat(task.getInterval()).isEqualTo(5000L);
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
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertySourcesPlaceholderConfigurer.class);
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
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertThat(fixedRateTasks.size()).isEqualTo(1);
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedRate");
		assertThat(task.getInitialDelay()).isEqualTo(1000L);
		assertThat(task.getInterval()).isEqualTo(3000L);
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
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks.size()).isEqualTo(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("x");
		assertThat(task.getExpression()).isEqualTo(businessHoursCronExpression);
	}

	@Test
	public void propertyPlaceholderForMetaAnnotation() {
		String businessHoursCronExpression = "0 0 9-17 * * MON-FRI";
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertySourcesPlaceholderConfigurer.class);
		Properties properties = new Properties();
		properties.setProperty("schedules.businessHours", businessHoursCronExpression);
		placeholderDefinition.getPropertyValues().addPropertyValue("properties", properties);
		BeanDefinition targetDefinition = new RootBeanDefinition(PropertyPlaceholderMetaAnnotationTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("placeholder", placeholderDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks.size()).isEqualTo(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("y");
		assertThat(task.getExpression()).isEqualTo(businessHoursCronExpression);
	}

	@Test
	public void nonVoidReturnType() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(NonVoidReturnTypeTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks().size()).isEqualTo(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks.size()).isEqualTo(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("cron");
		assertThat(task.getExpression()).isEqualTo("0 0 9-17 * * MON-FRI");
	}

	@Test
	public void emptyAnnotation() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(EmptyAnnotationTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				context::refresh);
	}

	@Test
	public void invalidCron() throws Throwable {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(InvalidCronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				context::refresh);
	}

	@Test
	public void nonEmptyParamList() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(NonEmptyParamListTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				context::refresh);
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
