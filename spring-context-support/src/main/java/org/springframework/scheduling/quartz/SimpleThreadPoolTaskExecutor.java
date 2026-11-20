/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.quartz;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.jspecify.annotations.Nullable;
import org.quartz.SchedulerConfigException;
import org.quartz.simpl.SimpleThreadPool;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;

/**
 * Subclass of Quartz's SimpleThreadPool that implements Spring's
 * {@link org.springframework.core.task.TaskExecutor} interface
 * and listens to Spring lifecycle callbacks.
 *
 * <p>Can be shared between a Quartz Scheduler (specified as "taskExecutor")
 * and other TaskExecutor users, or even used completely independent of
 * a Quartz Scheduler (as plain TaskExecutor backend).
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.quartz.simpl.SimpleThreadPool
 * @see org.springframework.core.task.TaskExecutor
 * @see SchedulerFactoryBean#setTaskExecutor
 */
@SuppressWarnings("deprecation")
public class SimpleThreadPoolTaskExecutor extends SimpleThreadPool
		implements AsyncTaskExecutor, SchedulingTaskExecutor, InitializingBean, DisposableBean {

	private boolean waitForJobsToCompleteOnShutdown = false;


	/**
	 * Set whether to wait for running jobs to complete on shutdown.
	 * Default is "false".
	 * @see org.quartz.simpl.SimpleThreadPool#shutdown(boolean)
	 */
	public void setWaitForJobsToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForJobsToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	@Override
	public void afterPropertiesSet() throws SchedulerConfigException {
		initialize();
	}


	@Override
	public void execute(Runnable task) {
		Assert.notNull(task, "Runnable must not be null");
		if (!runInThread(task)) {
			throw new SchedulingException("Quartz SimpleThreadPool already shut down");
		}
	}

	@Override
	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<>(task, null);
		execute(future);
		return future;
	}

	@Override
	public <T extends @Nullable Object> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future);
		return future;
	}


	@Override
	public void destroy() {
		shutdown(this.waitForJobsToCompleteOnShutdown);
	}

}
