/*
 * Copyright 2002-present the original author or authors.
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * @author Mark Fisher
 * @author Chris Beams
 */
@SuppressWarnings("unchecked")
public class ScheduledTasksBeanDefinitionParserTests {

	private ApplicationContext context;

	private ScheduledTaskRegistrar registrar;

	private Object testBean;


	@BeforeEach
	void setup() {
		this.context = new ClassPathXmlApplicationContext(
				"scheduledTasksContext.xml", ScheduledTasksBeanDefinitionParserTests.class);
		this.registrar = this.context.getBeansOfType(
				ScheduledTaskRegistrar.class).values().iterator().next();
		this.testBean = this.context.getBean("testBean");
	}

	@Test
	void checkScheduler() {
		Object schedulerBean = this.context.getBean("testScheduler");
		Object schedulerRef = new DirectFieldAccessor(this.registrar).getPropertyValue("taskScheduler");
		assertThat(schedulerRef).isEqualTo(schedulerBean);
	}

	@Test
	void checkTarget() {
		List<IntervalTask> tasks = (List<IntervalTask>) new DirectFieldAccessor(
				this.registrar).getPropertyValue("fixedRateTasks");
		Runnable runnable = tasks.get(0).getRunnable();

		ObjectAssert<ScheduledMethodRunnable> runnableAssert = assertThat(runnable)
				.extracting("runnable")
				.isInstanceOf(ScheduledMethodRunnable.class)
				.asInstanceOf(type(ScheduledMethodRunnable.class));
		runnableAssert.extracting("target").isEqualTo(testBean);
		runnableAssert.extracting("method.name").isEqualTo("test");
	}

	@Test
	void fixedRateTasks() {
		List<IntervalTask> tasks = (List<IntervalTask>) new DirectFieldAccessor(
				this.registrar).getPropertyValue("fixedRateTasks");
		assertThat(tasks).hasSize(3);
		assertThat(tasks.get(0).getIntervalDuration()).isEqualTo(Duration.ofMillis(1000L));
		assertThat(tasks.get(1).getIntervalDuration()).isEqualTo(Duration.ofMillis(2000L));
		assertThat(tasks.get(2).getIntervalDuration()).isEqualTo(Duration.ofMillis(4000L));
		assertThat(tasks.get(2).getInitialDelayDuration()).isEqualTo(Duration.ofMillis(500));
	}

	@Test
	void fixedDelayTasks() {
		List<IntervalTask> tasks = (List<IntervalTask>) new DirectFieldAccessor(
				this.registrar).getPropertyValue("fixedDelayTasks");
		assertThat(tasks).hasSize(2);
		assertThat(tasks.get(0).getIntervalDuration()).isEqualTo(Duration.ofMillis(3000L));
		assertThat(tasks.get(1).getIntervalDuration()).isEqualTo(Duration.ofMillis(3500L));
		assertThat(tasks.get(1).getInitialDelayDuration()).isEqualTo(Duration.ofMillis(250));
	}

	@Test
	void cronTasks() {
		List<CronTask> tasks = (List<CronTask>) new DirectFieldAccessor(
				this.registrar).getPropertyValue("cronTasks");
		assertThat(tasks).hasSize(1);
		assertThat(tasks.get(0).getExpression()).isEqualTo("*/4 * 9-17 * * MON-FRI");
	}

	@Test
	void triggerTasks() {
		List<TriggerTask> tasks = (List<TriggerTask>) new DirectFieldAccessor(
				this.registrar).getPropertyValue("triggerTasks");
		assertThat(tasks).hasSize(1);
		assertThat(tasks.get(0).getTrigger()).isInstanceOf(TestTrigger.class);
	}


	static class TestBean {

		public void test() {
		}
	}


	static class TestTrigger implements Trigger {

		@Override
		public Instant nextExecution(TriggerContext triggerContext) {
			return null;
		}
	}

}
