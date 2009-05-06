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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.Assert;

/**
 * Helper bean for registering tasks with a {@link TaskScheduler},
 * typically using cron expressions.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ScheduledTaskRegistrar implements InitializingBean, DisposableBean {

	private TaskScheduler taskScheduler;

	private Map<Runnable, Trigger> triggerTasks;

	private Map<Runnable, String> cronTasks;

	private Map<Runnable, Long> fixedRateTasks;

	private Map<Runnable, Long> fixedDelayTasks;

	private final Set<ScheduledFuture> scheduledFutures = new LinkedHashSet<ScheduledFuture>();


	public void setTaskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "TaskScheduler must not be null");
		this.taskScheduler = taskScheduler;
	}

	public void setScheduler(Object scheduler) {
		Assert.notNull(scheduler, "Scheduler object must not be null");
		if (scheduler instanceof TaskScheduler) {
			this.taskScheduler = (TaskScheduler) scheduler;
		}
		else if (scheduler instanceof ScheduledExecutorService) {
			this.taskScheduler = new ConcurrentTaskScheduler(((ScheduledExecutorService) scheduler));
		}
		else {
			throw new IllegalArgumentException("Unsupported scheduler type: " + scheduler.getClass());
		}
	}

	public void setTriggerTasks(Map<Runnable, Trigger> triggerTasks) {
		this.triggerTasks = triggerTasks;
	}

	public void setCronTasks(Map<Runnable, String> cronTasks) {
		this.cronTasks = cronTasks;
	}

	public void setFixedRateTasks(Map<Runnable, Long> fixedRateTasks) {
		this.fixedRateTasks = fixedRateTasks;
	}

	public void setFixedDelayTasks(Map<Runnable, Long> fixedDelayTasks) {
		this.fixedDelayTasks = fixedDelayTasks;
	}


	public void afterPropertiesSet() {
		if (this.taskScheduler == null) {
			this.taskScheduler = new ConcurrentTaskScheduler(Executors.newSingleThreadScheduledExecutor());
		}
		if (this.triggerTasks != null) {
			for (Map.Entry<Runnable, Trigger> entry : this.triggerTasks.entrySet()) {
				this.scheduledFutures.add(this.taskScheduler.schedule(entry.getKey(), entry.getValue()));
			}
		}
		if (this.cronTasks != null) {
			for (Map.Entry<Runnable, String> entry : cronTasks.entrySet()) {
				this.scheduledFutures.add(this.taskScheduler.schedule(entry.getKey(), new CronTrigger(entry.getValue())));
			}
		}
		if (this.fixedRateTasks != null) {
			for (Map.Entry<Runnable, Long> entry : this.fixedRateTasks.entrySet()) {
				this.scheduledFutures.add(this.taskScheduler.scheduleAtFixedRate(entry.getKey(), entry.getValue()));
			}
		}
		if (this.fixedDelayTasks != null) {
			for (Map.Entry<Runnable, Long> entry : this.fixedDelayTasks.entrySet()) {
				this.scheduledFutures.add(this.taskScheduler.scheduleWithFixedDelay(entry.getKey(), entry.getValue()));
			}
		}
	}


	public void destroy() {
		for (ScheduledFuture future : this.scheduledFutures) {
			future.cancel(true);
		}
	}

}
