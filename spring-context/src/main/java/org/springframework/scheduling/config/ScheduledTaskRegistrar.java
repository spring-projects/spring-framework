/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Helper bean for registering tasks with a {@link TaskScheduler}, typically using cron
 * expressions.
 *
 * <p>As of Spring 3.1, {@code ScheduledTaskRegistrar} has a more prominent user-facing
 * role when used in conjunction with the {@link
 * org.springframework.scheduling.annotation.EnableAsync @EnableAsync} annotation and its
 * {@link org.springframework.scheduling.annotation.SchedulingConfigurer
 * SchedulingConfigurer} callback interface.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Tobias Montagna-Hay
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.scheduling.annotation.EnableAsync
 * @see org.springframework.scheduling.annotation.SchedulingConfigurer
 */
public class ScheduledTaskRegistrar implements ScheduledTaskHolder, InitializingBean, DisposableBean {

	/**
	 * A special cron expression value that indicates a disabled trigger: {@value}.
	 * <p>This is primarily meant for use with {@link #addCronTask(Runnable, String)}
	 * when the value for the supplied {@code expression} is retrieved from an
	 * external source &mdash; for example, from a property in the
	 * {@link org.springframework.core.env.Environment Environment}.
	 * @since 5.2
	 * @see org.springframework.scheduling.annotation.Scheduled#CRON_DISABLED
	 */
	public static final String CRON_DISABLED = "-";


	@Nullable
	private TaskScheduler taskScheduler;

	@Nullable
	private ScheduledExecutorService localExecutor;

	@Nullable
	private List<TriggerTask> triggerTasks;

	@Nullable
	private List<CronTask> cronTasks;

	@Nullable
	private List<IntervalTask> fixedRateTasks;

	@Nullable
	private List<IntervalTask> fixedDelayTasks;

	private final Map<Task, ScheduledTask> unresolvedTasks = new HashMap<>(16);

	private final Set<ScheduledTask> scheduledTasks = new LinkedHashSet<>(16);


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
	public void setScheduler(@Nullable Object scheduler) {
		if (scheduler == null) {
			this.taskScheduler = null;
		}
		else if (scheduler instanceof TaskScheduler) {
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
	@Nullable
	public TaskScheduler getScheduler() {
		return this.taskScheduler;
	}


	/**
	 * Specify triggered tasks as a Map of Runnables (the tasks) and Trigger objects
	 * (typically custom implementations of the {@link Trigger} interface).
	 */
	public void setTriggerTasks(Map<Runnable, Trigger> triggerTasks) {
		this.triggerTasks = new ArrayList<>();
		triggerTasks.forEach((task, trigger) -> addTriggerTask(new TriggerTask(task, trigger)));
	}

	/**
	 * Specify triggered tasks as a list of {@link TriggerTask} objects. Primarily used
	 * by {@code <task:*>} namespace parsing.
	 * @since 3.2
	 * @see ScheduledTasksBeanDefinitionParser
	 */
	public void setTriggerTasksList(List<TriggerTask> triggerTasks) {
		this.triggerTasks = triggerTasks;
	}

	/**
	 * Get the trigger tasks as an unmodifiable list of {@link TriggerTask} objects.
	 * @return the list of tasks (never {@code null})
	 * @since 4.2
	 */
	public List<TriggerTask> getTriggerTaskList() {
		return (this.triggerTasks != null? Collections.unmodifiableList(this.triggerTasks) :
				Collections.emptyList());
	}

	/**
	 * Specify triggered tasks as a Map of Runnables (the tasks) and cron expressions.
	 * @see CronTrigger
	 */
	public void setCronTasks(Map<Runnable, String> cronTasks) {
		this.cronTasks = new ArrayList<>();
		cronTasks.forEach(this::addCronTask);
	}

	/**
	 * Specify triggered tasks as a list of {@link CronTask} objects. Primarily used by
	 * {@code <task:*>} namespace parsing.
	 * @since 3.2
	 * @see ScheduledTasksBeanDefinitionParser
	 */
	public void setCronTasksList(List<CronTask> cronTasks) {
		this.cronTasks = cronTasks;
	}

	/**
	 * Get the cron tasks as an unmodifiable list of {@link CronTask} objects.
	 * @return the list of tasks (never {@code null})
	 * @since 4.2
	 */
	public List<CronTask> getCronTaskList() {
		return (this.cronTasks != null ? Collections.unmodifiableList(this.cronTasks) :
				Collections.emptyList());
	}

	/**
	 * Specify triggered tasks as a Map of Runnables (the tasks) and fixed-rate values.
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void setFixedRateTasks(Map<Runnable, Long> fixedRateTasks) {
		this.fixedRateTasks = new ArrayList<>();
		fixedRateTasks.forEach(this::addFixedRateTask);
	}

	/**
	 * Specify fixed-rate tasks as a list of {@link IntervalTask} objects. Primarily used
	 * by {@code <task:*>} namespace parsing.
	 * @since 3.2
	 * @see ScheduledTasksBeanDefinitionParser
	 */
	public void setFixedRateTasksList(List<IntervalTask> fixedRateTasks) {
		this.fixedRateTasks = fixedRateTasks;
	}

	/**
	 * Get the fixed-rate tasks as an unmodifiable list of {@link IntervalTask} objects.
	 * @return the list of tasks (never {@code null})
	 * @since 4.2
	 */
	public List<IntervalTask> getFixedRateTaskList() {
		return (this.fixedRateTasks != null ? Collections.unmodifiableList(this.fixedRateTasks) :
				Collections.emptyList());
	}

	/**
	 * Specify triggered tasks as a Map of Runnables (the tasks) and fixed-delay values.
	 * @see TaskScheduler#scheduleWithFixedDelay(Runnable, long)
	 */
	public void setFixedDelayTasks(Map<Runnable, Long> fixedDelayTasks) {
		this.fixedDelayTasks = new ArrayList<>();
		fixedDelayTasks.forEach(this::addFixedDelayTask);
	}

	/**
	 * Specify fixed-delay tasks as a list of {@link IntervalTask} objects. Primarily used
	 * by {@code <task:*>} namespace parsing.
	 * @since 3.2
	 * @see ScheduledTasksBeanDefinitionParser
	 */
	public void setFixedDelayTasksList(List<IntervalTask> fixedDelayTasks) {
		this.fixedDelayTasks = fixedDelayTasks;
	}

	/**
	 * Get the fixed-delay tasks as an unmodifiable list of {@link IntervalTask} objects.
	 * @return the list of tasks (never {@code null})
	 * @since 4.2
	 */
	public List<IntervalTask> getFixedDelayTaskList() {
		return (this.fixedDelayTasks != null ? Collections.unmodifiableList(this.fixedDelayTasks) :
				Collections.emptyList());
	}


	/**
	 * Add a Runnable task to be triggered per the given {@link Trigger}.
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void addTriggerTask(Runnable task, Trigger trigger) {
		addTriggerTask(new TriggerTask(task, trigger));
	}

	/**
	 * Add a {@code TriggerTask}.
	 * @since 3.2
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void addTriggerTask(TriggerTask task) {
		if (this.triggerTasks == null) {
			this.triggerTasks = new ArrayList<>();
		}
		this.triggerTasks.add(task);
	}

	/**
	 * Add a {@link Runnable} task to be triggered per the given cron {@code expression}.
	 * <p>As of Spring Framework 5.2, this method will not register the task if the
	 * {@code expression} is equal to {@link #CRON_DISABLED}.
	 */
	public void addCronTask(Runnable task, String expression) {
		if (!CRON_DISABLED.equals(expression)) {
			addCronTask(new CronTask(task, expression));
		}
	}

	/**
	 * Add a {@link CronTask}.
	 * @since 3.2
	 */
	public void addCronTask(CronTask task) {
		if (this.cronTasks == null) {
			this.cronTasks = new ArrayList<>();
		}
		this.cronTasks.add(task);
	}

	/**
	 * Add a {@code Runnable} task to be triggered at the given fixed-rate interval.
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void addFixedRateTask(Runnable task, long interval) {
		addFixedRateTask(new IntervalTask(task, interval, 0));
	}

	/**
	 * Add a fixed-rate {@link IntervalTask}.
	 * @since 3.2
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
	 */
	public void addFixedRateTask(IntervalTask task) {
		if (this.fixedRateTasks == null) {
			this.fixedRateTasks = new ArrayList<>();
		}
		this.fixedRateTasks.add(task);
	}

	/**
	 * Add a Runnable task to be triggered with the given fixed delay.
	 * @see TaskScheduler#scheduleWithFixedDelay(Runnable, long)
	 */
	public void addFixedDelayTask(Runnable task, long delay) {
		addFixedDelayTask(new IntervalTask(task, delay, 0));
	}

	/**
	 * Add a fixed-delay {@link IntervalTask}.
	 * @since 3.2
	 * @see TaskScheduler#scheduleWithFixedDelay(Runnable, long)
	 */
	public void addFixedDelayTask(IntervalTask task) {
		if (this.fixedDelayTasks == null) {
			this.fixedDelayTasks = new ArrayList<>();
		}
		this.fixedDelayTasks.add(task);
	}


	/**
	 * Return whether this {@code ScheduledTaskRegistrar} has any tasks registered.
	 * @since 3.2
	 */
	public boolean hasTasks() {
		return (!CollectionUtils.isEmpty(this.triggerTasks) ||
				!CollectionUtils.isEmpty(this.cronTasks) ||
				!CollectionUtils.isEmpty(this.fixedRateTasks) ||
				!CollectionUtils.isEmpty(this.fixedDelayTasks));
	}


	/**
	 * Calls {@link #scheduleTasks()} at bean construction time.
	 */
	@Override
	public void afterPropertiesSet() {
		scheduleTasks();
	}

	/**
	 * Schedule all registered tasks against the underlying
	 * {@linkplain #setTaskScheduler(TaskScheduler) task scheduler}.
	 */
	@SuppressWarnings("deprecation")
	protected void scheduleTasks() {
		if (this.taskScheduler == null) {
			this.localExecutor = Executors.newSingleThreadScheduledExecutor();
			this.taskScheduler = new ConcurrentTaskScheduler(this.localExecutor);
		}
		if (this.triggerTasks != null) {
			for (TriggerTask task : this.triggerTasks) {
				addScheduledTask(scheduleTriggerTask(task));
			}
		}
		if (this.cronTasks != null) {
			for (CronTask task : this.cronTasks) {
				addScheduledTask(scheduleCronTask(task));
			}
		}
		if (this.fixedRateTasks != null) {
			for (IntervalTask task : this.fixedRateTasks) {
				addScheduledTask(scheduleFixedRateTask(task));
			}
		}
		if (this.fixedDelayTasks != null) {
			for (IntervalTask task : this.fixedDelayTasks) {
				addScheduledTask(scheduleFixedDelayTask(task));
			}
		}
	}

	private void addScheduledTask(@Nullable ScheduledTask task) {
		if (task != null) {
			this.scheduledTasks.add(task);
		}
	}


	/**
	 * Schedule the specified trigger task, either right away if possible
	 * or on initialization of the scheduler.
	 * @return a handle to the scheduled task, allowing to cancel it
	 * @since 4.3
	 */
	@Nullable
	public ScheduledTask scheduleTriggerTask(TriggerTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask(task);
			newTask = true;
		}
		if (this.taskScheduler != null) {
			scheduledTask.future = this.taskScheduler.schedule(task.getRunnable(), task.getTrigger());
		}
		else {
			addTriggerTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}

	/**
	 * Schedule the specified cron task, either right away if possible
	 * or on initialization of the scheduler.
	 * @return a handle to the scheduled task, allowing to cancel it
	 * (or {@code null} if processing a previously registered task)
	 * @since 4.3
	 */
	@Nullable
	public ScheduledTask scheduleCronTask(CronTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask(task);
			newTask = true;
		}
		if (this.taskScheduler != null) {
			scheduledTask.future = this.taskScheduler.schedule(task.getRunnable(), task.getTrigger());
		}
		else {
			addCronTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}

	/**
	 * Schedule the specified fixed-rate task, either right away if possible
	 * or on initialization of the scheduler.
	 * @return a handle to the scheduled task, allowing to cancel it
	 * (or {@code null} if processing a previously registered task)
	 * @since 4.3
	 * @deprecated as of 5.0.2, in favor of {@link #scheduleFixedRateTask(FixedRateTask)}
	 */
	@Deprecated
	@Nullable
	public ScheduledTask scheduleFixedRateTask(IntervalTask task) {
		FixedRateTask taskToUse = (task instanceof FixedRateTask ? (FixedRateTask) task :
				new FixedRateTask(task.getRunnable(), task.getInterval(), task.getInitialDelay()));
		return scheduleFixedRateTask(taskToUse);
	}

	/**
	 * Schedule the specified fixed-rate task, either right away if possible
	 * or on initialization of the scheduler.
	 * @return a handle to the scheduled task, allowing to cancel it
	 * (or {@code null} if processing a previously registered task)
	 * @since 5.0.2
	 */
	@Nullable
	public ScheduledTask scheduleFixedRateTask(FixedRateTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask(task);
			newTask = true;
		}
		if (this.taskScheduler != null) {
			if (task.getInitialDelay() > 0) {
				Date startTime = new Date(this.taskScheduler.getClock().millis() + task.getInitialDelay());
				scheduledTask.future =
						this.taskScheduler.scheduleAtFixedRate(task.getRunnable(), startTime, task.getInterval());
			}
			else {
				scheduledTask.future =
						this.taskScheduler.scheduleAtFixedRate(task.getRunnable(), task.getInterval());
			}
		}
		else {
			addFixedRateTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}

	/**
	 * Schedule the specified fixed-delay task, either right away if possible
	 * or on initialization of the scheduler.
	 * @return a handle to the scheduled task, allowing to cancel it
	 * (or {@code null} if processing a previously registered task)
	 * @since 4.3
	 * @deprecated as of 5.0.2, in favor of {@link #scheduleFixedDelayTask(FixedDelayTask)}
	 */
	@Deprecated
	@Nullable
	public ScheduledTask scheduleFixedDelayTask(IntervalTask task) {
		FixedDelayTask taskToUse = (task instanceof FixedDelayTask ? (FixedDelayTask) task :
				new FixedDelayTask(task.getRunnable(), task.getInterval(), task.getInitialDelay()));
		return scheduleFixedDelayTask(taskToUse);
	}

	/**
	 * Schedule the specified fixed-delay task, either right away if possible
	 * or on initialization of the scheduler.
	 * @return a handle to the scheduled task, allowing to cancel it
	 * (or {@code null} if processing a previously registered task)
	 * @since 5.0.2
	 */
	@Nullable
	public ScheduledTask scheduleFixedDelayTask(FixedDelayTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask(task);
			newTask = true;
		}
		if (this.taskScheduler != null) {
			if (task.getInitialDelay() > 0) {
				Date startTime = new Date(this.taskScheduler.getClock().millis() + task.getInitialDelay());
				scheduledTask.future =
						this.taskScheduler.scheduleWithFixedDelay(task.getRunnable(), startTime, task.getInterval());
			}
			else {
				scheduledTask.future =
						this.taskScheduler.scheduleWithFixedDelay(task.getRunnable(), task.getInterval());
			}
		}
		else {
			addFixedDelayTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}


	/**
	 * Return all locally registered tasks that have been scheduled by this registrar.
	 * @since 5.0.2
	 * @see #addTriggerTask
	 * @see #addCronTask
	 * @see #addFixedRateTask
	 * @see #addFixedDelayTask
	 */
	@Override
	public Set<ScheduledTask> getScheduledTasks() {
		return Collections.unmodifiableSet(this.scheduledTasks);
	}

	@Override
	public void destroy() {
		for (ScheduledTask task : this.scheduledTasks) {
			task.cancel();
		}
		if (this.localExecutor != null) {
			this.localExecutor.shutdownNow();
		}
	}

}
