/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.HashMap;
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
 * <p>As of Spring 3.1, {@code ScheduledTaskRegistrar} has a more prominent user-facing
 * role when used in conjunction with the @{@link
 * org.springframework.scheduling.annotation.EnableAsync EnableAsync} annotation and its
 * {@link org.springframework.scheduling.annotation.SchedulingConfigurer
 * SchedulingConfigurer} callback interface.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.scheduling.annotation.EnableAsync
 * @see org.springframework.scheduling.annotation.SchedulingConfigurer
 */
public class ScheduledTaskRegistrar implements InitializingBean, DisposableBean {

	private TaskScheduler taskScheduler;

	private ScheduledExecutorService localExecutor;

	private Map<Runnable, Trigger> triggerTasks;

	private Map<Runnable, String> cronTasks;

	private Map<Runnable, Long> fixedRateTasks;

	private Map<Runnable, Long> fixedDelayTasks;

	private final Set<ScheduledFuture<?>> scheduledFutures = new LinkedHashSet<ScheduledFuture<?>>();


	/**
	 * Set the {@link TaskScheduler} to register scheduled tasks with.
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "TaskScheduler must not be null");
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Set the {@link TaskScheduler} to register scheduled tasks with, or a
	 * {@link java.util.concurrent.ScheduledExecutorService} to be wrapped as a
	 * {@code TaskScheduler}.
	 */
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

	/**
	 * Return the {@link TaskScheduler} instance for this registrar (may be {@code null}).
	 */
	public TaskScheduler getScheduler() {
		return this.taskScheduler;
	}

	/**
	 * Specify triggered tasks as a Map of Runnables (the tasks) and Trigger objects
	 * (typically custom implementations of the {@link Trigger} interface).
	 */
	public void setTriggerTasks(Map<Runnable, Trigger> triggerTasks) {
		this.triggerTasks = triggerTasks;
	}

	/**
	 * Specify triggered tasks as a Map of Runnables (the tasks) and cron expressions.
	 * @see CronTrigger
	 */
	public void setCronTasks(Map<Runnable, String> cronTasks) {
		this.cronTasks = cronTasks;
	}

	/**
	 * Specify triggered tasks as a Map of Runnables (the tasks) and fixed-rate values.
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void setFixedRateTasks(Map<Runnable, Long> fixedRateTasks) {
		this.fixedRateTasks = fixedRateTasks;
	}

	/**
	 * Specify triggered tasks as a Map of Runnables (the tasks) and fixed-delay values.
	 * @see TaskScheduler#scheduleWithFixedDelay(Runnable, long)
	 */
	public void setFixedDelayTasks(Map<Runnable, Long> fixedDelayTasks) {
		this.fixedDelayTasks = fixedDelayTasks;
	}

	/**
	 * Add a Runnable task to be triggered per the given {@link Trigger}.
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void addTriggerTask(Runnable task, Trigger trigger) {
		if (this.triggerTasks == null) {
			this.triggerTasks = new HashMap<Runnable, Trigger>();
		}
		this.triggerTasks.put(task, trigger);
	}

	/**
	 * Add a Runnable task to be triggered per the given cron expression
	 */
	public void addCronTask(Runnable task, String cronExpression) {
		if (this.cronTasks == null) {
			this.cronTasks = new HashMap<Runnable, String>();
		}
		this.cronTasks.put(task, cronExpression);
	}

	/**
	 * Add a Runnable task to be triggered at the given fixed-rate period.
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void addFixedRateTask(Runnable task, long period) {
		if (this.fixedRateTasks == null) {
			this.fixedRateTasks = new HashMap<Runnable, Long>();
		}
		this.fixedRateTasks.put(task, period);
	}

	/**
	 * Add a Runnable task to be triggered with the given fixed delay.
	 * @see TaskScheduler#scheduleWithFixedDelay(Runnable, long)
	 */
	public void addFixedDelayTask(Runnable task, long delay) {
		if (this.fixedDelayTasks == null) {
			this.fixedDelayTasks = new HashMap<Runnable, Long>();
		}
		this.fixedDelayTasks.put(task, delay);
	}

	/**
	 * Schedule all registered tasks against the underlying {@linkplain
	 * #setTaskScheduler(TaskScheduler) task scheduler.
	 */
	public void afterPropertiesSet() {
		if (this.taskScheduler == null) {
			this.localExecutor = Executors.newSingleThreadScheduledExecutor();
			this.taskScheduler = new ConcurrentTaskScheduler(this.localExecutor);
		}
		if (this.triggerTasks != null) {
			for (Map.Entry<Runnable, Trigger> entry : this.triggerTasks.entrySet()) {
				this.scheduledFutures.add(this.taskScheduler.schedule(entry.getKey(), entry.getValue()));
			}
		}
		if (this.cronTasks != null) {
			for (Map.Entry<Runnable, String> entry : this.cronTasks.entrySet()) {
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
		for (ScheduledFuture<?> future : this.scheduledFutures) {
			future.cancel(true);
		}
		if (this.localExecutor != null) {
			this.localExecutor.shutdownNow();
		}
	}

}
