/*
 * Copyright 2002-2024 the original author or authors.
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.TestGroup.LONG_RUNNING;

/**
 * Tests use of @EnableScheduling on @Configuration classes.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 3.1
 */
class EnableSchedulingTests {

	private AnnotationConfigApplicationContext ctx;

	private static final AtomicBoolean shutdownFailure = new AtomicBoolean();


	@BeforeEach
	void reset() {
		shutdownFailure.set(false);
	}

	@AfterEach
	void tearDown() {
		if (ctx != null) {
			ctx.close();
		}
		assertThat(shutdownFailure).isFalse();
	}


	/*
	 * Tests compatibility between default executor in TaskSchedulerRouter
	 * and explicit ThreadPoolTaskScheduler in configuration subclass.
	 */
	@ParameterizedTest
	@ValueSource(classes = {FixedRateTaskConfig.class, FixedRateTaskConfigSubclass.class})
	@EnabledForTestGroups(LONG_RUNNING)
	void withFixedRateTask(Class<?> configClass) throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(configClass);
		assertThat(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()).hasSize(2);

		Thread.sleep(110);
		assertThat(ctx.getBean(AtomicInteger.class).get()).isGreaterThanOrEqualTo(10);
	}

	/*
	 * Tests compatibility between SimpleAsyncTaskScheduler in regular configuration
	 * and explicit ThreadPoolTaskScheduler in configuration subclass. This includes
	 * pause/resume behavior and a controlled shutdown with a 1s termination timeout.
	 */
	@ParameterizedTest
	@ValueSource(classes = {ExplicitSchedulerConfig.class, ExplicitSchedulerConfigSubclass.class})
	@Timeout(2)  // should actually complete within 1s
	@EnabledForTestGroups(LONG_RUNNING)
	void withExplicitScheduler(Class<?> configClass) throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(configClass);
		assertThat(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()).hasSize(1);

		Thread.sleep(110);
		ctx.stop();
		int count1 = ctx.getBean(AtomicInteger.class).get();
		assertThat(count1).isGreaterThanOrEqualTo(10).isLessThan(20);
		Thread.sleep(110);
		int count2 = ctx.getBean(AtomicInteger.class).get();
		assertThat(count2).isGreaterThanOrEqualTo(10).isLessThan(20);
		ctx.start();
		Thread.sleep(110);
		int count3 = ctx.getBean(AtomicInteger.class).get();
		assertThat(count3).isGreaterThanOrEqualTo(20);

		TaskExecutor executor = ctx.getBean(TaskExecutor.class);
		AtomicInteger count = new AtomicInteger(0);
		for (int i = 0; i < 2; i++) {
			executor.execute(() -> {
				try {
					Thread.sleep(10000);  // try to break test timeout
				}
				catch (InterruptedException ex) {
					// expected during executor shutdown
					try {
						Thread.sleep(500);
						// should get here within task termination timeout (1000)
						count.incrementAndGet();
					}
					catch (InterruptedException ex2) {
						// not expected
					}
				}
			});
		}

		assertThat(ctx.getBean(ExplicitSchedulerConfig.class).threadName).startsWith("explicitScheduler-");
		assertThat(Arrays.asList(ctx.getDefaultListableBeanFactory().getDependentBeans("myTaskScheduler"))
				.contains(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();

		// Include executor shutdown in test timeout (2 seconds),
		// expecting interruption of the sleeping thread...
		ctx.close();
		assertThat(count.intValue()).isEqualTo(2);
	}

	@Test
	void withExplicitSchedulerAmbiguity_andSchedulingEnabled() {
		// No exception raised as of 4.3, aligned with the behavior for @Async methods (SPR-14030)
		ctx = new AnnotationConfigApplicationContext(AmbiguousExplicitSchedulerConfig.class);
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withExplicitScheduledTaskRegistrar() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(ExplicitScheduledTaskRegistrarConfig.class);
		assertThat(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()).hasSize(1);

		Thread.sleep(110);
		assertThat(ctx.getBean(AtomicInteger.class).get()).isGreaterThanOrEqualTo(10);
		assertThat(ctx.getBean(ExplicitScheduledTaskRegistrarConfig.class).threadName).startsWith("explicitScheduler1");
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withQualifiedScheduler() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(QualifiedExplicitSchedulerConfig.class);
		assertThat(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()).hasSize(1);

		Thread.sleep(110);
		assertThat(ctx.getBean(AtomicInteger.class).get()).isGreaterThanOrEqualTo(10);
		assertThat(ctx.getBean(QualifiedExplicitSchedulerConfig.class).threadName).startsWith("explicitScheduler1");
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withQualifiedSchedulerAndPlaceholder() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(QualifiedExplicitSchedulerConfigWithPlaceholder.class);
		assertThat(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()).hasSize(1);

		Thread.sleep(110);
		assertThat(ctx.getBean(AtomicInteger.class).get()).isGreaterThanOrEqualTo(10);
		assertThat(ctx.getBean(QualifiedExplicitSchedulerConfigWithPlaceholder.class).threadName)
				.startsWith("explicitScheduler1").isNotEqualTo("explicitScheduler1-1");
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withQualifiedSchedulerWithFixedDelayTask() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(QualifiedExplicitSchedulerConfigWithFixedDelayTask.class);
		assertThat(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()).hasSize(1);

		Thread.sleep(110);
		assertThat(ctx.getBean(AtomicInteger.class).get()).isBetween(4, 5);
		assertThat(ctx.getBean(QualifiedExplicitSchedulerConfigWithFixedDelayTask.class).threadName)
				.isEqualTo("explicitScheduler1-1");
	}

	@Test
	void withAmbiguousTaskSchedulers_butNoActualTasks() {
		ctx = new AnnotationConfigApplicationContext(SchedulingEnabled_withAmbiguousTaskSchedulers_butNoActualTasks.class);
	}

	@Test
	void withAmbiguousTaskSchedulers_andSingleTask() {
		// No exception raised as of 4.3, aligned with the behavior for @Async methods (SPR-14030)
		ctx = new AnnotationConfigApplicationContext(SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask.class);
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withAmbiguousTaskSchedulers_andSingleTask_disambiguatedByScheduledTaskRegistrarBean() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(
				SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask_disambiguatedByScheduledTaskRegistrar.class);

		Thread.sleep(110);
		assertThat(ctx.getBean(ThreadAwareWorker.class).executedByThread).startsWith("explicitScheduler2-");
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withAmbiguousTaskSchedulers_andSingleTask_disambiguatedBySchedulerNameAttribute() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(
				SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask_disambiguatedBySchedulerNameAttribute.class);

		Thread.sleep(110);
		assertThat(ctx.getBean(ThreadAwareWorker.class).executedByThread).startsWith("explicitScheduler2-");
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withTaskAddedVia_configureTasks() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(SchedulingEnabled_withTaskAddedVia_configureTasks.class);

		Thread.sleep(110);
		assertThat(ctx.getBean(ThreadAwareWorker.class).executedByThread).startsWith("taskScheduler-");
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withInitiallyDelayedFixedRateTask() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(FixedRateTaskConfig_withInitialDelay.class);

		Thread.sleep(1950);
		AtomicInteger counter = ctx.getBean(AtomicInteger.class);

		// The @Scheduled method should have been called several times
		// but not more times than the delay allows.
		assertThat(counter.get()).isBetween(6, 10);
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withInitiallyDelayedFixedDelayTask() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(FixedDelayTaskConfig_withInitialDelay.class);

		Thread.sleep(1950);
		AtomicInteger counter = ctx.getBean(AtomicInteger.class);

		// The @Scheduled method should have been called several times
		// but not more times than the delay allows.
		assertThat(counter.get()).isBetween(1, 5);
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withPrototypeContainedFixedDelayTask() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(FixedDelayTaskConfig_withPrototypeBean.class);

		ctx.getBean(PrototypeBeanWithScheduled.class);
		Thread.sleep(1950);
		AtomicInteger counter = ctx.getBean(AtomicInteger.class);

		// The @Scheduled method should have been called several times
		// but not more times than the delay allows.
		assertThat(counter.get()).isBetween(1, 5);
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withPrototypeFactoryContainedFixedDelayTask() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(FixedDelayTaskConfig_withFactoryBean.class);

		ctx.getBean(PrototypeBeanWithScheduled.class);
		Thread.sleep(1950);
		AtomicInteger counter = ctx.getBean(AtomicInteger.class);

		// The @Scheduled method should have been called several times
		// but not more times than the delay allows.
		assertThat(counter.get()).isBetween(1, 5);
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withOneTimeTask() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(OneTimeTaskConfig.class);

		Thread.sleep(110);
		AtomicInteger counter = ctx.getBean(AtomicInteger.class);

		// The @Scheduled method should have been called exactly once.
		assertThat(counter.get()).isEqualTo(1);
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withTriggerTask() throws InterruptedException {
		ctx = new AnnotationConfigApplicationContext(TriggerTaskConfig.class);

		Thread.sleep(110);
		assertThat(ctx.getBean(AtomicInteger.class).get()).isGreaterThan(1);
	}


	@Configuration
	@EnableScheduling
	static class FixedRateTaskConfig implements SchedulingConfigurer {

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.addFixedRateTask(() -> {}, Duration.ofMillis(100));
		}

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Scheduled(fixedRate = 10)
		public void task() {
			counter().incrementAndGet();
		}
	}


	@Configuration
	static class FixedRateTaskConfigSubclass extends FixedRateTaskConfig {

		@Bean
		public TaskScheduler taskScheduler() {
			return new ThreadPoolTaskScheduler();
		}
	}


	@Configuration
	@EnableScheduling
	static class ExplicitSchedulerConfig {

		String threadName;

		@Bean
		public TaskScheduler myTaskScheduler() {
			SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler-");
			scheduler.setTaskTerminationTimeout(1000);
			return scheduler;
		}

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Scheduled(fixedRate = 10)
		public void task() {
			threadName = Thread.currentThread().getName();
			counter().incrementAndGet();
		}
	}


	@Configuration
	static class ExplicitSchedulerConfigSubclass extends ExplicitSchedulerConfig {

		@Bean
		@Override
		public TaskScheduler myTaskScheduler() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setPoolSize(2);
			scheduler.setThreadNamePrefix("explicitScheduler-");
			scheduler.setAcceptTasksAfterContextClose(true);
			scheduler.setAwaitTerminationMillis(1000);
			return scheduler;
		}
	}


	@Configuration
	@EnableScheduling
	static class AmbiguousExplicitSchedulerConfig {

		@Bean
		public TaskScheduler taskScheduler1() {
			SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1-");
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2-");
			return scheduler;
		}

		@Scheduled(fixedRate = 10)
		public void task() {
		}
	}


	@Configuration
	@EnableScheduling
	static class ExplicitScheduledTaskRegistrarConfig implements SchedulingConfigurer {

		String threadName;

		@Bean
		public TaskScheduler taskScheduler1() {
			SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1-");
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2-");
			return scheduler;
		}

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Scheduled(fixedRate = 10)
		public void task() {
			threadName = Thread.currentThread().getName();
			counter().incrementAndGet();
		}

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.setScheduler(taskScheduler1());
		}
	}


	@Configuration
	@EnableScheduling
	static class QualifiedExplicitSchedulerConfig {

		String threadName;

		@Bean @Qualifier("myScheduler")
		public TaskScheduler taskScheduler1() {
			SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1-");
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2-");
			return scheduler;
		}

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Scheduled(fixedRate = 10, scheduler = "myScheduler")
		public void task() throws InterruptedException {
			threadName = Thread.currentThread().getName();
			counter().incrementAndGet();
			Thread.sleep(10);
		}
	}


	@Configuration
	@EnableScheduling
	static class QualifiedExplicitSchedulerConfigWithPlaceholder {

		String threadName;

		@Bean @Qualifier("myScheduler")
		public TaskScheduler taskScheduler1() {
			SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1-");
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2-");
			return scheduler;
		}

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Scheduled(fixedRate = 10, scheduler = "${scheduler}")
		public void task() throws InterruptedException {
			threadName = Thread.currentThread().getName();
			counter().incrementAndGet();
			Thread.sleep(10);
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
			PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
			Properties props = new Properties();
			props.setProperty("scheduler", "myScheduler");
			pspc.setProperties(props);
			return pspc;
		}
	}


	@Configuration
	@EnableScheduling
	static class QualifiedExplicitSchedulerConfigWithFixedDelayTask {

		String threadName;

		@Bean @Qualifier("myScheduler")
		public TaskScheduler taskScheduler1() {
			SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1-");
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2-");
			return scheduler;
		}

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Scheduled(fixedDelay = 10, scheduler = "myScheduler")
		public void task() throws InterruptedException {
			threadName = Thread.currentThread().getName();
			counter().incrementAndGet();
			Thread.sleep(10);
		}
	}


	@Configuration
	@EnableScheduling
	static class SchedulingEnabled_withAmbiguousTaskSchedulers_butNoActualTasks {

		@Bean
		public TaskScheduler taskScheduler1() {
			SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1-");
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2-");
			return scheduler;
		}
	}


	@Configuration
	@EnableScheduling
	static class SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask {

		@Scheduled(fixedRate = 10L)
		public void task() {
		}

		@Bean
		public TaskScheduler taskScheduler1() {
			SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1-");
			scheduler.setConcurrencyLimit(1);
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2-");
			return scheduler;
		}
	}


	static class ThreadAwareWorker {

		String executedByThread;
	}


	@Configuration
	@EnableScheduling
	static class SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask_disambiguatedByScheduledTaskRegistrar implements SchedulingConfigurer {

		@Scheduled(fixedRate = 10)
		public void task() {
			worker().executedByThread = Thread.currentThread().getName();
		}

		@Bean
		public ThreadAwareWorker worker() {
			return new ThreadAwareWorker();
		}

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.setScheduler(taskScheduler2());
		}

		@Bean
		public TaskScheduler taskScheduler1() {
			SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1-");
			scheduler.setConcurrencyLimit(1);
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2-");
			return scheduler;
		}
	}


	@Configuration
	@EnableScheduling
	static class SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask_disambiguatedBySchedulerNameAttribute implements SchedulingConfigurer {

		@Scheduled(fixedRate = 10)
		public void task() {
			worker().executedByThread = Thread.currentThread().getName();
		}

		@Bean
		public ThreadAwareWorker worker() {
			return new ThreadAwareWorker();
		}

		@Bean
		public TaskScheduler taskScheduler1() {
			SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1-");
			scheduler.setConcurrencyLimit(1);
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2-");
			return scheduler;
		}

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.setScheduler(taskScheduler2());
		}
	}


	@Configuration
	@EnableScheduling
	static class SchedulingEnabled_withTaskAddedVia_configureTasks implements SchedulingConfigurer {

		@Bean
		public ThreadAwareWorker worker() {
			return new ThreadAwareWorker();
		}

		@Bean
		public TaskScheduler taskScheduler() {
			return new ThreadPoolTaskScheduler();
		}

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.setScheduler(taskScheduler());
			taskRegistrar.addFixedRateTask(new IntervalTask(
					() -> worker().executedByThread = Thread.currentThread().getName(),
					Duration.ofMillis(10)));
		}
	}


	@Configuration
	@EnableScheduling
	static class FixedRateTaskConfig_withInitialDelay {

		@Autowired
		ScheduledAnnotationBeanPostProcessor bpp;

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Scheduled(initialDelay = 1000, fixedRate = 100)
		public void task() throws InterruptedException {
			counter().incrementAndGet();
			Thread.sleep(100);
		}

		@PreDestroy
		public void validateLateCancellation() {
			if (this.bpp.getScheduledTasks().isEmpty()) {
				shutdownFailure.set(true);
			}
		}
	}


	@Configuration
	@EnableScheduling
	static class FixedDelayTaskConfig_withInitialDelay {

		@Autowired
		ScheduledAnnotationBeanPostProcessor bpp;

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Scheduled(initialDelay = 1000, fixedDelay = 100)
		public void task() throws InterruptedException {
			counter().incrementAndGet();
			Thread.sleep(100);
		}

		@PreDestroy
		public void validateLateCancellation() {
			if (this.bpp.getScheduledTasks().isEmpty()) {
				shutdownFailure.set(true);
			}
		}
	}


	@Configuration
	@EnableScheduling
	static class FixedDelayTaskConfig_withPrototypeBean {

		@Autowired
		ScheduledAnnotationBeanPostProcessor bpp;

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Bean @Scope("prototype")
		public PrototypeBeanWithScheduled prototypeBean() {
			return new PrototypeBeanWithScheduled(counter());
		}

		@PreDestroy
		public void validateEarlyCancellation() {
			if (!this.bpp.getScheduledTasks().isEmpty()) {
				shutdownFailure.set(true);
			}
		}
	}


	@Configuration
	@EnableScheduling
	static class FixedDelayTaskConfig_withFactoryBean {

		@Autowired
		ScheduledAnnotationBeanPostProcessor bpp;

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Bean
		public FactoryBeanForScheduled prototypeBean() {
			return new FactoryBeanForScheduled(counter());
		}

		@PreDestroy
		public void validateEarlyCancellation() {
			if (!this.bpp.getScheduledTasks().isEmpty()) {
				shutdownFailure.set(true);
			}
		}
	}


	static class PrototypeBeanWithScheduled {

		private AtomicInteger counter;

		public PrototypeBeanWithScheduled(AtomicInteger counter) {
			this.counter = counter;
		}

		@Scheduled(initialDelay = 1000, fixedDelay = 100)
		public void task() throws InterruptedException {
			this.counter.incrementAndGet();
			Thread.sleep(100);
		}
	}


	static class FactoryBeanForScheduled implements FactoryBean<PrototypeBeanWithScheduled> {

		private AtomicInteger counter;

		public FactoryBeanForScheduled(AtomicInteger counter) {
			this.counter = counter;
		}

		@Override
		public PrototypeBeanWithScheduled getObject() {
			return new PrototypeBeanWithScheduled(this.counter);
		}

		@Override
		public Class<?> getObjectType() {
			return PrototypeBeanWithScheduled.class;
		}
	}


	@Configuration
	@EnableScheduling
	static class OneTimeTaskConfig {

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Scheduled(initialDelay = 10)
		public void task() {
			counter().incrementAndGet();
		}
	}


	@Configuration
	static class TriggerTaskConfig {

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Bean
		public TaskScheduler scheduler() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.initialize();
			scheduler.schedule(() -> counter().incrementAndGet(),
					triggerContext -> Instant.now().plus(10, ChronoUnit.MILLIS));
			return scheduler;
		}
	}

}
