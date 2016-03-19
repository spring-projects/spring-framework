/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * Tests use of @EnableScheduling on @Configuration classes.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.1
 */
public class EnableSchedulingTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void withFixedRateTask() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(FixedRateTaskConfig.class);

		Thread.sleep(100);
		assertThat(ctx.getBean(AtomicInteger.class).get(), greaterThanOrEqualTo(10));
		ctx.close();
	}


	@Configuration
	@EnableScheduling
	static class FixedRateTaskConfig {

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Scheduled(fixedRate = 10)
		public void task() {
			counter().incrementAndGet();
		}
	}


	@Test
	public void withSubclass() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(FixedRateTaskConfigSubclass.class);

		Thread.sleep(100);
		assertThat(ctx.getBean(AtomicInteger.class).get(), greaterThanOrEqualTo(10));
		ctx.close();
	}


	@Configuration
	static class FixedRateTaskConfigSubclass extends FixedRateTaskConfig {
	}


	@Test
	public void withExplicitScheduler() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ExplicitSchedulerConfig.class);

		Thread.sleep(100);
		assertThat(ctx.getBean(AtomicInteger.class).get(), greaterThanOrEqualTo(10));
		assertThat(ctx.getBean(ExplicitSchedulerConfig.class).threadName, startsWith("explicitScheduler-"));
		ctx.close();
	}


	@Configuration
	@EnableScheduling
	static class ExplicitSchedulerConfig {

		String threadName;

		@Bean
		public TaskScheduler taskScheduler() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler-");
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


	@Ignore("Disabled until SPR-14030 is resolved")
	@Test
	@SuppressWarnings("resource")
	public void withExplicitSchedulerAmbiguity_andSchedulingEnabled() {
		exception.expect(IllegalStateException.class);
		exception.expectMessage(startsWith("More than one TaskScheduler bean exists within the context"));
		new AnnotationConfigApplicationContext(AmbiguousExplicitSchedulerConfig.class);
	}


	@Configuration
	@EnableScheduling
	static class AmbiguousExplicitSchedulerConfig {

		@Bean
		public TaskScheduler taskScheduler1() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1");
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2");
			return scheduler;
		}

		@Scheduled(fixedRate = 10)
		public void task() {
		}
	}


	@Test
	public void withExplicitScheduledTaskRegistrar() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				ExplicitScheduledTaskRegistrarConfig.class);

		Thread.sleep(100);
		assertThat(ctx.getBean(AtomicInteger.class).get(), greaterThanOrEqualTo(10));
		assertThat(ctx.getBean(ExplicitScheduledTaskRegistrarConfig.class).threadName, startsWith("explicitScheduler1"));
		ctx.close();
	}


	@Configuration
	@EnableScheduling
	static class ExplicitScheduledTaskRegistrarConfig implements SchedulingConfigurer {

		String threadName;

		@Bean
		public TaskScheduler taskScheduler1() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1");
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2");
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

		public Object getScheduler() {
			return null;
		}

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.setScheduler(taskScheduler1());
		}
	}


	@Test
	public void withAmbiguousTaskSchedulers_butNoActualTasks() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
			SchedulingEnabled_withAmbiguousTaskSchedulers_butNoActualTasks.class);
		ctx.close();
	}


	@Configuration
	@EnableScheduling
	static class SchedulingEnabled_withAmbiguousTaskSchedulers_butNoActualTasks {

		@Bean
		public TaskScheduler taskScheduler1() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1");
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2");
			return scheduler;
		}
	}


	@Ignore("Disabled until SPR-14030 is resolved")
	@Test
	@SuppressWarnings("resource")
	public void withAmbiguousTaskSchedulers_andSingleTask() {
		exception.expect(IllegalStateException.class);
		exception.expectMessage(startsWith("More than one TaskScheduler bean exists within the context"));
		new AnnotationConfigApplicationContext(SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask.class);
	}


	@Configuration
	@EnableScheduling
	static class SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask {

		@Scheduled(fixedRate = 10L)
		public void task() {
		}

		@Bean
		public TaskScheduler taskScheduler1() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1");
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler2() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler2");
			return scheduler;
		}
	}


	@Test
	public void withAmbiguousTaskSchedulers_andSingleTask_disambiguatedByScheduledTaskRegistrarBean() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask_disambiguatedByScheduledTaskRegistrar.class);

		Thread.sleep(20);
		assertThat(ctx.getBean(ThreadAwareWorker.class).executedByThread, startsWith("explicitScheduler2-"));
		ctx.close();
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
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
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


	@Test
	public void withAmbiguousTaskSchedulers_andSingleTask_disambiguatedBySchedulerNameAttribute() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask_disambiguatedBySchedulerNameAttribute.class);

		Thread.sleep(20);
		assertThat(ctx.getBean(ThreadAwareWorker.class).executedByThread, startsWith("explicitScheduler2-"));
		ctx.close();
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
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("explicitScheduler1-");
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


	@Test
	public void withTaskAddedVia_configureTasks() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				SchedulingEnabled_withTaskAddedVia_configureTasks.class);

		Thread.sleep(20);
		assertThat(ctx.getBean(ThreadAwareWorker.class).executedByThread, startsWith("taskScheduler-"));
		ctx.close();
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
					new Runnable() {
						@Override
						public void run() {
							worker().executedByThread = Thread.currentThread().getName();
						}
					},
					10, 0));
		}
	}


	@Test
	public void withTriggerTask() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(TriggerTaskConfig.class);

		Thread.sleep(100);
		assertThat(ctx.getBean(AtomicInteger.class).get(), greaterThan(1));
		ctx.close();
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
			scheduler.schedule(
				new Runnable() {
					@Override
					public void run() {
						counter().incrementAndGet();
					}
				},
				new Trigger() {
					@Override
					public Date nextExecutionTime(TriggerContext triggerContext) {
						return new Date(new Date().getTime()+10);
					}
				});
			return scheduler;
		}
	}


	@Test
	public void withInitiallyDelayedFixedRateTask() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				FixedRateTaskConfig_withInitialDelay.class);

		Thread.sleep(1950);
		AtomicInteger counter = ctx.getBean(AtomicInteger.class);
		ctx.close();

		// The @Scheduled method should have been called at least once but
		// not more times than the delay allows.
		assertThat(counter.get(), both(greaterThan(0)).and(lessThanOrEqualTo(10)));
	}


	@Configuration
	@EnableScheduling
	static class FixedRateTaskConfig_withInitialDelay {

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Scheduled(initialDelay = 1000, fixedRate = 100)
		public void task() {
			counter().incrementAndGet();
		}
	}

}
