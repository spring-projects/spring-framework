/*
 * Copyright 2002-2023 the original author or authors.
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;

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
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Tests for {@link ScheduledAnnotationBeanPostProcessor}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Stevo Slavić
 * @author Victor Brown
 */
class ScheduledAnnotationBeanPostProcessorTests {

	private final StaticApplicationContext context = new StaticApplicationContext();


	@AfterEach
	void closeContextAfterTest() {
		context.close();
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
		FixedDelay, 5_000
		FixedDelayInSeconds, 5_000
		FixedDelayInMinutes, 180_000
		FixedDelayWithMaxValue, -1
	""")
	void fixedDelayTask(@NameToClass Class<?> beanClass, long expectedInterval) {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(beanClass);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedDelayTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedDelayTasks");
		assertThat(fixedDelayTasks).hasSize(1);
		IntervalTask task = fixedDelayTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedDelay");
		assertThat(task.getInitialDelayDuration()).isZero();
		assertThat(task.getIntervalDuration()).isEqualTo(
				Duration.ofMillis(expectedInterval < 0 ? Long.MAX_VALUE : expectedInterval));
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
		FixedRate, 3_000
		FixedRateInSeconds, 5_000
		FixedRateInMinutes, 180_000
	""")
	void fixedRateTask(@NameToClass Class<?> beanClass, long expectedInterval) {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(beanClass);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertThat(fixedRateTasks).hasSize(1);
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedRate");
		assertSoftly(softly -> {
			softly.assertThat(task.getInitialDelayDuration()).as("initial delay").isZero();
			softly.assertThat(task.getIntervalDuration()).as("interval").isEqualTo(Duration.ofMillis(expectedInterval));
		});
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
		FixedRateWithInitialDelay, 1_000, 3_000
		FixedRateWithInitialDelayInSeconds, 5_000, 3_000
		FixedRateWithInitialDelayInMinutes, 60_000, 180_000
	""")
	void fixedRateTaskWithInitialDelay(@NameToClass Class<?> beanClass, long expectedInitialDelay, long expectedInterval) {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(beanClass);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertThat(fixedRateTasks).hasSize(1);
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedRate");
		assertSoftly(softly -> {
			softly.assertThat(task.getInitialDelayDuration()).as("initial delay").isEqualTo(Duration.ofMillis(expectedInitialDelay));
			softly.assertThat(task.getIntervalDuration()).as("interval").isEqualTo(Duration.ofMillis(expectedInterval));
		});
	}

	@Test
	void severalFixedRatesWithRepeatedScheduledAnnotation() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(SeveralFixedRatesWithRepeatedScheduledAnnotationTestBean.class);
		severalFixedRates(context, processorDefinition, targetDefinition);
	}

	@Test
	void severalFixedRatesWithSchedulesContainerAnnotation() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(SeveralFixedRatesWithSchedulesContainerAnnotationTestBean.class);
		severalFixedRates(context, processorDefinition, targetDefinition);
	}

	@Test
	void severalFixedRatesOnBaseClass() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(FixedRatesSubBean.class);
		severalFixedRates(context, processorDefinition, targetDefinition);
	}

	@Test
	void severalFixedRatesOnDefaultMethod() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(FixedRatesDefaultBean.class);
		severalFixedRates(context, processorDefinition, targetDefinition);
	}

	@Test
	void severalFixedRatesAgainstNestedCglibProxy() {
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
		assertThat(postProcessor.getScheduledTasks()).hasSize(2);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertThat(fixedRateTasks).hasSize(2);
		IntervalTask task1 = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable1 = (ScheduledMethodRunnable) task1.getRunnable();
		Object targetObject = runnable1.getTarget();
		Method targetMethod = runnable1.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedRate");
		assertThat(task1.getInitialDelayDuration()).isZero();
		assertThat(task1.getIntervalDuration()).isEqualTo(Duration.ofMillis(4_000L));
		IntervalTask task2 = fixedRateTasks.get(1);
		ScheduledMethodRunnable runnable2 = (ScheduledMethodRunnable) task2.getRunnable();
		targetObject = runnable2.getTarget();
		targetMethod = runnable2.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedRate");
		assertThat(task2.getInitialDelayDuration()).isEqualTo(Duration.ofMillis(2_000L));
		assertThat(task2.getIntervalDuration()).isEqualTo(Duration.ofMillis(4_000L));
	}

	@Test
	void cronTask() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(CronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks).hasSize(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("cron");
		assertThat(task.getExpression()).isEqualTo("*/7 * * * * ?");
	}

	@Test
	void cronTaskWithZone() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(CronWithTimezoneTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks).hasSize(1);
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
		ZonedDateTime dateTime = ZonedDateTime.of(2013, 4, 15, 4, 0, 0, 0, ZoneId.of("GMT+10"));
//		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+10"));
//		cal.clear();
//		cal.set(2013, 3, 15, 4, 0);  // 15-04-2013 4:00 GMT+10;
		Instant lastScheduledExecution = dateTime.toInstant();
		Instant lastActualExecution = dateTime.toInstant();
		dateTime = dateTime.plusMinutes(30);
		Instant lastCompletion = dateTime.toInstant();
		TriggerContext triggerContext = new SimpleTriggerContext(
				lastScheduledExecution, lastActualExecution, lastCompletion);
		dateTime = dateTime.plusMinutes(90); // 6:00
		Instant nextExecutionTime = cronTrigger.nextExecution(triggerContext);
		// assert that 6:00 is next execution time
		assertThat(nextExecutionTime).isEqualTo(dateTime.toInstant());
	}

	@Test
	void cronTaskWithInvalidZone() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(CronWithInvalidTimezoneTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(context::refresh);
	}

	@Test
	void cronTaskWithMethodValidation() {
		BeanDefinition validationDefinition = new RootBeanDefinition(MethodValidationPostProcessor.class);
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(CronTestBean.class);
		context.registerBeanDefinition("methodValidation", validationDefinition);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(context::refresh);
	}

	@Test
	void cronTaskWithScopedProxy() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		new AnnotatedBeanDefinitionReader(context).register(ProxiedCronTestBean.class, ProxiedCronTestBeanDependent.class);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks).hasSize(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(context.getBean(ScopedProxyUtils.getTargetBeanName("target")));
		assertThat(targetMethod.getName()).isEqualTo("cron");
		assertThat(task.getExpression()).isEqualTo("*/7 * * * * ?");
	}

	@Test
	void metaAnnotationWithFixedRate() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(MetaAnnotationFixedRateTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertThat(fixedRateTasks).hasSize(1);
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("checkForUpdates");
		assertThat(task.getIntervalDuration()).isEqualTo(Duration.ofMillis(5_000L));
	}

	@Test
	void composedAnnotationWithInitialDelayAndFixedRate() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(ComposedAnnotationFixedRateTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertThat(fixedRateTasks).hasSize(1);
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("checkForUpdates");
		assertThat(task.getIntervalDuration()).isEqualTo(Duration.ofMillis(5_000L));
		assertThat(task.getInitialDelayDuration()).isEqualTo(Duration.ofMillis(1_000L));
	}

	@Test
	void metaAnnotationWithCronExpression() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(MetaAnnotationCronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks).hasSize(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("generateReport");
		assertThat(task.getExpression()).isEqualTo("0 0 * * * ?");
	}

	@Test
	void propertyPlaceholderWithCron() {
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
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks).hasSize(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("x");
		assertThat(task.getExpression()).isEqualTo(businessHoursCronExpression);
	}

	@Test
	void propertyPlaceholderWithInactiveCron() {
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

	@ParameterizedTest
	@CsvSource(textBlock = """
		PropertyPlaceholderWithFixedDelay, 5000, 1000, 5_000, 1_000
		PropertyPlaceholderWithFixedDelay, PT5S, PT1S, 5_000, 1_000
		PropertyPlaceholderWithFixedDelayInSeconds, 5000, 1000, 5_000_000, 1_000_000
		PropertyPlaceholderWithFixedDelayInSeconds, PT5S, PT1S, 5_000, 1_000
	""")
	void propertyPlaceholderWithFixedDelay(@NameToClass Class<?> beanClass, String fixedDelay, String initialDelay,
			long expectedInterval, long expectedInitialDelay) {

		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertySourcesPlaceholderConfigurer.class);
		Properties properties = new Properties();
		properties.setProperty("fixedDelay", fixedDelay);
		properties.setProperty("initialDelay", initialDelay);
		placeholderDefinition.getPropertyValues().addPropertyValue("properties", properties);
		BeanDefinition targetDefinition = new RootBeanDefinition(beanClass);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("placeholder", placeholderDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedDelayTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedDelayTasks");
		assertThat(fixedDelayTasks).hasSize(1);
		IntervalTask task = fixedDelayTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedDelay");
		assertSoftly(softly -> {
			softly.assertThat(task.getInitialDelayDuration()).as("initial delay").isEqualTo(Duration.ofMillis(expectedInitialDelay));
			softly.assertThat(task.getIntervalDuration()).as("interval").isEqualTo(Duration.ofMillis(expectedInterval));
		});
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
		PropertyPlaceholderWithFixedRate, 3000, 1000, 3_000, 1_000
		PropertyPlaceholderWithFixedRate, PT3S, PT1S, 3_000, 1_000
		PropertyPlaceholderWithFixedRateInSeconds, 3000, 1000, 3_000_000, 1_000_000
		PropertyPlaceholderWithFixedRateInSeconds, PT3S, PT1S, 3_000, 1_000
	""")
	void propertyPlaceholderWithFixedRate(@NameToClass Class<?> beanClass, String fixedRate, String initialDelay,
			long expectedInterval, long expectedInitialDelay) {

		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition placeholderDefinition = new RootBeanDefinition(PropertySourcesPlaceholderConfigurer.class);
		Properties properties = new Properties();
		properties.setProperty("fixedRate", fixedRate);
		properties.setProperty("initialDelay", initialDelay);
		placeholderDefinition.getPropertyValues().addPropertyValue("properties", properties);
		BeanDefinition targetDefinition = new RootBeanDefinition(beanClass);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("placeholder", placeholderDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<IntervalTask> fixedRateTasks = (List<IntervalTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("fixedRateTasks");
		assertThat(fixedRateTasks).hasSize(1);
		IntervalTask task = fixedRateTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("fixedRate");
		assertSoftly(softly -> {
			softly.assertThat(task.getInitialDelayDuration()).as("initial delay").isEqualTo(Duration.ofMillis(expectedInitialDelay));
			softly.assertThat(task.getIntervalDuration()).as("interval").isEqualTo(Duration.ofMillis(expectedInterval));
		});
	}

	@Test
	void expressionWithCron() {
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
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks).hasSize(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("x");
		assertThat(task.getExpression()).isEqualTo(businessHoursCronExpression);
	}

	@Test
	void propertyPlaceholderForMetaAnnotation() {
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
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks).hasSize(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("y");
		assertThat(task.getExpression()).isEqualTo(businessHoursCronExpression);
	}

	@Test
	void nonVoidReturnType() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(NonVoidReturnTypeTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.refresh();

		ScheduledTaskHolder postProcessor = context.getBean("postProcessor", ScheduledTaskHolder.class);
		assertThat(postProcessor.getScheduledTasks()).hasSize(1);

		Object target = context.getBean("target");
		ScheduledTaskRegistrar registrar = (ScheduledTaskRegistrar)
				new DirectFieldAccessor(postProcessor).getPropertyValue("registrar");
		@SuppressWarnings("unchecked")
		List<CronTask> cronTasks = (List<CronTask>)
				new DirectFieldAccessor(registrar).getPropertyValue("cronTasks");
		assertThat(cronTasks).hasSize(1);
		CronTask task = cronTasks.get(0);
		ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task.getRunnable();
		Object targetObject = runnable.getTarget();
		Method targetMethod = runnable.getMethod();
		assertThat(targetObject).isEqualTo(target);
		assertThat(targetMethod.getName()).isEqualTo("cron");
		assertThat(task.getExpression()).isEqualTo("0 0 9-17 * * MON-FRI");
	}

	@Test
	void emptyAnnotation() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(EmptyAnnotationTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(context::refresh);
	}

	@Test
	void invalidCron() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(InvalidCronTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(context::refresh);
	}

	@Test
	void nonEmptyParamList() {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(NonEmptyParamListTestBean.class);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(context::refresh);
	}


	static class FixedDelay {

		@Scheduled(fixedDelay = 5_000)
		void fixedDelay() {
		}
	}

	static class FixedDelayInSeconds {

		@Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
		void fixedDelay() {
		}
	}

	static class FixedDelayInMinutes {

		@Scheduled(fixedDelay = 3, timeUnit = TimeUnit.MINUTES)
		void fixedDelay() {
		}
	}

	static class FixedDelayWithMaxValue {

		@Scheduled(fixedDelay = Long.MAX_VALUE)
		void fixedDelay() {
		}
	}


	static class FixedRate {

		@Scheduled(fixedRate = 3_000)
		void fixedRate() {
		}
	}

	static class FixedRateInSeconds {

		@Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
		void fixedRate() {
		}
	}

	static class FixedRateInMinutes {

		@Scheduled(fixedRate = 3, timeUnit = TimeUnit.MINUTES)
		void fixedRate() {
		}
	}


	static class FixedRateWithInitialDelay {

		@Scheduled(fixedRate = 3_000, initialDelay = 1_000)
		void fixedRate() {
		}
	}

	static class FixedRateWithInitialDelayInSeconds {

		@Scheduled(fixedRate = 3, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
		void fixedRate() {
		}
	}

	static class FixedRateWithInitialDelayInMinutes {

		@Scheduled(fixedRate = 3, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
		void fixedRate() {
		}
	}


	static class SeveralFixedRatesWithSchedulesContainerAnnotationTestBean {

		@Schedules({@Scheduled(fixedRate = 4_000), @Scheduled(fixedRate = 4_000, initialDelay = 2_000)})
		void fixedRate() {
		}
	}


	static class SeveralFixedRatesWithRepeatedScheduledAnnotationTestBean {

		@Scheduled(fixedRate = 4_000)
		@Scheduled(fixedRate = 4_000, initialDelay = 2_000)
		void fixedRate() {
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

		@Scheduled(fixedRate = 4_000)
		@Scheduled(fixedRate = 4_000, initialDelay = 2_000)
		void fixedRate() {
		}
	}


	static class FixedRatesSubBean extends FixedRatesBaseBean {
	}


	interface FixedRatesDefaultMethod {

		@Scheduled(fixedRate = 4_000)
		@Scheduled(fixedRate = 4_000, initialDelay = 2_000)
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
		void cron() throws IOException {
			throw new IOException("no no no");
		}
	}


	@Component("target")
	@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
	static class ProxiedCronTestBean {

		@Scheduled(cron = "*/7 * * * * ?")
		void cron() throws IOException {
			throw new IOException("no no no");
		}
	}


	static class ProxiedCronTestBeanDependent {

		ProxiedCronTestBeanDependent(ProxiedCronTestBean testBean) {
		}
	}


	static class NonVoidReturnTypeTestBean {

		@Scheduled(cron = "0 0 9-17 * * MON-FRI")
		String cron() {
			return "oops";
		}
	}


	static class EmptyAnnotationTestBean {

		@Scheduled
		void invalid() {
		}
	}


	static class InvalidCronTestBean {

		@Scheduled(cron = "abc")
		void invalid() {
		}
	}


	static class NonEmptyParamListTestBean {

		@Scheduled(fixedRate = 3_000)
		void invalid(String oops) {
		}
	}


	@Scheduled(fixedRate = 5_000)
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface EveryFiveSeconds {
	}

	@Scheduled(cron = "0 0 * * * ?")
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface Hourly {
	}

	@Scheduled(initialDelay = 1_000)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface WaitASec {

		@AliasFor(annotation = Scheduled.class)
		long fixedDelay() default -1;

		@AliasFor(annotation = Scheduled.class)
		long fixedRate() default -1;
	}


	static class MetaAnnotationFixedRateTestBean {

		@EveryFiveSeconds
		void checkForUpdates() {
		}
	}


	static class ComposedAnnotationFixedRateTestBean {

		@WaitASec(fixedRate = 5_000)
		void checkForUpdates() {
		}
	}


	static class MetaAnnotationCronTestBean {

		@Hourly
		void generateReport() {
		}
	}


	static class PropertyPlaceholderWithCronTestBean {

		@Scheduled(cron = "${schedules.businessHours}")
		void x() {
		}
	}


	static class PropertyPlaceholderWithFixedDelay {

		@Scheduled(fixedDelayString = "${fixedDelay}", initialDelayString = "${initialDelay}")
		void fixedDelay() {
		}
	}

	static class PropertyPlaceholderWithFixedDelayInSeconds {

		@Scheduled(fixedDelayString = "${fixedDelay}", initialDelayString = "${initialDelay}", timeUnit = TimeUnit.SECONDS)
		void fixedDelay() {
		}
	}


	static class PropertyPlaceholderWithFixedRate {

		@Scheduled(fixedRateString = "${fixedRate}", initialDelayString = "${initialDelay}")
		void fixedRate() {
		}
	}

	static class PropertyPlaceholderWithFixedRateInSeconds {

		@Scheduled(fixedRateString = "${fixedRate}", initialDelayString = "${initialDelay}", timeUnit = TimeUnit.SECONDS)
		void fixedRate() {
		}
	}


	static class ExpressionWithCronTestBean {

		@Scheduled(cron = "#{schedules.businessHours}")
		void x() {
		}
	}


	@Scheduled(cron = "${schedules.businessHours}")
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface BusinessHours {
	}


	static class PropertyPlaceholderMetaAnnotationTestBean {

		@BusinessHours
		void y() {
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@ConvertWith(NameToClass.Converter.class)
	private @interface NameToClass {
		class Converter implements ArgumentConverter {
			@Override
			public Class<?> convert(Object beanClassName, ParameterContext context) throws ArgumentConversionException {
				try {
					String name = getClass().getEnclosingClass().getEnclosingClass().getName() + "$" + beanClassName;
					return getClass().getClassLoader().loadClass(name);
				}
				catch (Exception ex) {
					throw new ArgumentConversionException("Failed to convert class name to Class", ex);
				}
			}
		}
	}

}
