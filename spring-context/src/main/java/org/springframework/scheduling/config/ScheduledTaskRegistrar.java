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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
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
 * <p>{@code ScheduledTaskRegistrar} has a more prominent user-facing role when used in
 * conjunction with the {@link
 * org.springframework.scheduling.annotation.EnableAsync @EnableAsync} annotation and its
 * {@link org.springframework.scheduling.annotation.SchedulingConfigurer
 * SchedulingConfigurer} callback interface.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Tobias Montagna-Hay
 * @author Sam Brannen
 * @author Arjen Poutsma
 * @author Brian Clozel
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


	private @Nullable TaskScheduler taskScheduler;

	private @Nullable ScheduledExecutorService localExecutor;

	private @Nullable ObservationRegistry observationRegistry;

	private @Nullable List<TriggerTask> triggerTasks;

	private @Nullable List<CronTask> cronTasks;

	private @Nullable List<IntervalTask> fixedRateTasks;

	private @Nullable List<IntervalTask> fixedDelayTasks;

	private @Nullable List<DelayedTask> oneTimeTasks;

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
		else if (scheduler instanceof TaskScheduler ts) {
			this.taskScheduler = ts;
		}
		else if (scheduler instanceof ScheduledExecutorService ses) {
			this.taskScheduler = new ConcurrentTaskScheduler(ses);
		}
		else {
			throw new IllegalArgumentException("Unsupported scheduler type: " + scheduler.getClass());
		}
	}

	/**
	 * Return the {@link TaskScheduler} instance for this registrar (may be {@code null}).
	 */
	public @Nullable TaskScheduler getScheduler() {
		return this.taskScheduler;
	}

	/**
	 * Configure an {@link ObservationRegistry} to record observations for scheduled tasks.
	 * @since 6.1
	 */
	public void setObservationRegistry(@Nullable ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	/**
	 * Return the {@link ObservationRegistry} for this registrar.
	 * @since 6.1
	 */
	public @Nullable ObservationRegistry getObservationRegistry() {
		return this.observationRegistry;
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
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, Duration)
	 */
	public void setFixedRateTasks(Map<Runnable, Long> fixedRateTasks) {
		this.fixedRateTasks = new ArrayList<>();
		fixedRateTasks.forEach((task, interval) -> addFixedRateTask(task, Duration.ofMillis(interval)));
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
	 * @see TaskScheduler#scheduleWithFixedDelay(Runnable, Duration)
	 */
	public void setFixedDelayTasks(Map<Runnable, Long> fixedDelayTasks) {
		this.fixedDelayTasks = new ArrayList<>();
		fixedDelayTasks.forEach((task, delay) -> addFixedDelayTask(task, Duration.ofMillis(delay)));
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
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, Duration)
	 */
	public void addTriggerTask(Runnable task, Trigger trigger) {
		addTriggerTask(new TriggerTask(task, trigger));
	}

	/**
	 * Add a {@code TriggerTask}.
	 * @since 3.2
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, Duration)
	 */
	public void addTriggerTask(TriggerTask task) {
		if (this.triggerTasks == null) {
			this.triggerTasks = new ArrayList<>();
		}
		this.triggerTasks.add(task);
	}

	/**
	 * Add a {@link Runnable} task to be triggered per the given cron {@code expression}.
	 * <p>This method will not register the task if the {@code expression} is
	 * equal to {@link #CRON_DISABLED}.
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
	 * @deprecated as of 6.0, in favor of {@link #addFixedRateTask(Runnable, Duration)}
	 */
	@Deprecated(since = "6.0")
	public void addFixedRateTask(Runnable task, long interval) {
		addFixedRateTask(new IntervalTask(task, interval));
	}

	/**
	 * Add a {@code Runnable} task to be triggered at the given fixed-rate interval.
	 * @since 6.0
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, Duration)
	 */
	public void addFixedRateTask(Runnable task, Duration interval) {
		addFixedRateTask(new IntervalTask(task, interval));
	}

	/**
	 * Add a fixed-rate {@link IntervalTask}.
	 * @since 3.2
	 * @see TaskScheduler#scheduleAtFixedRate(Runnable, Duration)
	 * @see FixedRateTask
	 */
	public void addFixedRateTask(IntervalTask task) {
		if (this.fixedRateTasks == null) {
			this.fixedRateTasks = new ArrayList<>();
		}
		this.fixedRateTasks.add(task);
	}

	/**
	 * Add a Runnable task to be triggered with the given fixed delay.
	 * @deprecated as of 6.0, in favor of {@link #addFixedDelayTask(Runnable, Duration)}
	 */
	@Deprecated(since = "6.0")
	public void addFixedDelayTask(Runnable task, long interval) {
		addFixedDelayTask(new IntervalTask(task, interval));
	}

	/**
	 * Add a Runnable task to be triggered with the given fixed delay.
	 * @since 6.0
	 * @see TaskScheduler#scheduleWithFixedDelay(Runnable, Duration)
	 */
	public void addFixedDelayTask(Runnable task, Duration interval) {
		addFixedDelayTask(new IntervalTask(task, interval));
	}

	/**
	 * Add a fixed-delay {@link IntervalTask}.
	 * @since 3.2
	 * @see TaskScheduler#scheduleWithFixedDelay(Runnable, Duration)
	 * @see FixedDelayTask
	 */
	public void addFixedDelayTask(IntervalTask task) {
		if (this.fixedDelayTasks == null) {
			this.fixedDelayTasks = new ArrayList<>();
		}
		this.fixedDelayTasks.add(task);
	}

	/**
	 * Add a Runnable task to be triggered once after the given initial delay.
	 * @since 6.1
	 * @see TaskScheduler#schedule(Runnable, Instant)
	 */
	public void addOneTimeTask(Runnable task, Duration initialDelay) {
		addOneTimeTask(new OneTimeTask(task, initialDelay));
	}

	/**
	 * Add a one-time {@link DelayedTask}.
	 * @since 6.1
	 * @see TaskScheduler#schedule(Runnable, Instant)
	 * @see OneTimeTask
	 */
	public void addOneTimeTask(DelayedTask task) {
		if (this.oneTimeTasks == null) {
			this.oneTimeTasks = new ArrayList<>();
		}
		this.oneTimeTasks.add(task);
	}


	/**
	 * Return whether this {@code ScheduledTaskRegistrar} has any tasks registered.
	 * @since 3.2
	 */
	public boolean hasTasks() {
		return (!CollectionUtils.isEmpty(this.triggerTasks) ||
				!CollectionUtils.isEmpty(this.cronTasks) ||
				!CollectionUtils.isEmpty(this.fixedRateTasks) ||
				!CollectionUtils.isEmpty(this.fixedDelayTasks) ||
				!CollectionUtils.isEmpty(this.oneTimeTasks));
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
				if (task instanceof FixedRateTask fixedRateTask) {
					addScheduledTask(scheduleFixedRateTask(fixedRateTask));
				}
				else {
					addScheduledTask(scheduleFixedRateTask(new FixedRateTask(task)));
				}
			}
		}
		if (this.fixedDelayTasks != null) {
			for (IntervalTask task : this.fixedDelayTasks) {
				if (task instanceof FixedDelayTask fixedDelayTask) {
					addScheduledTask(scheduleFixedDelayTask(fixedDelayTask));
				}
				else {
					addScheduledTask(scheduleFixedDelayTask(new FixedDelayTask(task)));
				}
			}
		}
		if (this.oneTimeTasks != null) {
			for (DelayedTask task : this.oneTimeTasks) {
				if (task instanceof OneTimeTask oneTimeTask) {
					addScheduledTask(scheduleOneTimeTask(oneTimeTask));
				}
				else {
					addScheduledTask(scheduleOneTimeTask(new OneTimeTask(task)));
				}
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
	public @Nullable ScheduledTask scheduleTriggerTask(TriggerTask task) {
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
	public @Nullable ScheduledTask scheduleCronTask(CronTask task) {
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
	 * @since 5.0.2
	 */
	public @Nullable ScheduledTask scheduleFixedRateTask(FixedRateTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask(task);
			newTask = true;
		}
		if (this.taskScheduler != null) {
			Duration initialDelay = task.getInitialDelayDuration();
			if (initialDelay.toNanos() > 0) {
				Instant startTime = this.taskScheduler.getClock().instant().plus(initialDelay);
				scheduledTask.future =
						this.taskScheduler.scheduleAtFixedRate(task.getRunnable(), startTime, task.getIntervalDuration());
			}
			else {
				scheduledTask.future =
						this.taskScheduler.scheduleAtFixedRate(task.getRunnable(), task.getIntervalDuration());
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
	 * @since 5.0.2
	 */
	public @Nullable ScheduledTask scheduleFixedDelayTask(FixedDelayTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask(task);
			newTask = true;
		}
		if (this.taskScheduler != null) {
			Duration initialDelay = task.getInitialDelayDuration();
			if (!initialDelay.isNegative()) {
				Instant startTime = this.taskScheduler.getClock().instant().plus(task.getInitialDelayDuration());
				scheduledTask.future =
						this.taskScheduler.scheduleWithFixedDelay(task.getRunnable(), startTime, task.getIntervalDuration());
			}
			else {
				scheduledTask.future =
						this.taskScheduler.scheduleWithFixedDelay(task.getRunnable(), task.getIntervalDuration());
			}
		}
		else {
			addFixedDelayTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}

	/**
	 * Schedule the specified one-time task, either right away if possible
	 * or on initialization of the scheduler.
	 * @return a handle to the scheduled task, allowing to cancel it
	 * (or {@code null} if processing a previously registered task)
	 * @since 6.1
	 */
	public @Nullable ScheduledTask scheduleOneTimeTask(OneTimeTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask(task);
			newTask = true;
		}
		if (this.taskScheduler != null) {
			Instant startTime = this.taskScheduler.getClock().instant().plus(task.getInitialDelayDuration());
			scheduledTask.future = this.taskScheduler.schedule(task.getRunnable(), startTime);
		}
		else {
			addOneTimeTask(task);
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
			task.cancel(false);
		}
		if (this.localExecutor != null) {
			this.localExecutor.shutdownNow();
		}
	}

}
