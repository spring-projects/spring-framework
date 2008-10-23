/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.scheduling.backportconcurrent;

import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionHandler;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.support.DelegatingExceptionProofRunnable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that sets up
 * a JSR-166 backport
 * {@link edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService}
 * (by default:
 * {@link edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor}
 * as implementation) and exposes it for bean references.
 *
 * <p>Allows for registration of {@link ScheduledExecutorTask ScheduledExecutorTasks},
 * automatically starting the {@link ScheduledExecutorService} on initialization and
 * cancelling it on destruction of the context. In scenarios that just require static
 * registration of tasks at startup, there is no need to access the
 * {@link ScheduledExecutorService} instance itself in application code.
 *
 * <p>Note that
 * {@link edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService}
 * uses a {@link Runnable} instance that is shared between repeated executions,
 * in contrast to Quartz which instantiates a new Job for each execution.
 *
 * <p><b>WARNING:</b> {@link Runnable Runnables} submitted via a native
 * {@link java.util.concurrent.ScheduledExecutorService} are removed from
 * the execution schedule once they throw an exception. If you would prefer
 * to continue execution after such an exception, switch this FactoryBean's
 * {@link #setContinueScheduledExecutionAfterException "continueScheduledExecutionAfterException"}
 * property to "true".
 *
 * <p>This class is analogous to the
 * {@link org.springframework.scheduling.timer.TimerFactoryBean}
 * class for the JDK {@link java.util.Timer} facility.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see ScheduledExecutorTask
 * @see edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService
 * @see edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor
 * @see org.springframework.scheduling.timer.TimerFactoryBean
 */
public class ScheduledExecutorFactoryBean implements FactoryBean, BeanNameAware, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private int poolSize = 1;

	private ThreadFactory threadFactory = Executors.defaultThreadFactory();

	private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

	private boolean exposeUnconfigurableExecutor = false;

	private ScheduledExecutorTask[] scheduledExecutorTasks;

	private boolean continueScheduledExecutionAfterException = false;

	private boolean waitForTasksToCompleteOnShutdown = false;

	private String beanName;

	private ScheduledExecutorService executor;


	/**
	 * Set the ScheduledExecutorService's pool size.
	 * Default is 1.
	 */
	public void setPoolSize(int poolSize) {
		Assert.isTrue(poolSize > 0, "'poolSize' must be 1 or higher");
		this.poolSize = poolSize;
	}

	/**
	 * Set the ThreadFactory to use for the ThreadPoolExecutor's thread pool.
	 * Default is the ThreadPoolExecutor's default thread factory.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.Executors#defaultThreadFactory()
	 */
	public void setThreadFactory(ThreadFactory threadFactory) {
		this.threadFactory = (threadFactory != null ? threadFactory : Executors.defaultThreadFactory());
	}

	/**
	 * Set the RejectedExecutionHandler to use for the ThreadPoolExecutor.
	 * Default is the ThreadPoolExecutor's default abort policy.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor.AbortPolicy
	 */
	public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
		this.rejectedExecutionHandler =
				(rejectedExecutionHandler != null ? rejectedExecutionHandler : new ThreadPoolExecutor.AbortPolicy());
	}

	/**
	 * Specify whether this FactoryBean should expose an unconfigurable
	 * decorator for the created executor.
	 * <p>Default is "false", exposing the raw executor as bean reference.
	 * Switch this flag to "true" to strictly prevent clients from
	 * modifying the executor's configuration.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.Executors#unconfigurableScheduledExecutorService
	 */
	public void setExposeUnconfigurableExecutor(boolean exposeUnconfigurableExecutor) {
		this.exposeUnconfigurableExecutor = exposeUnconfigurableExecutor;
	}

	/**
	 * Register a list of ScheduledExecutorTask objects with the ScheduledExecutorService
	 * that this FactoryBean creates. Depending on each ScheduledExecutorTask's settings,
	 * it will be registered via one of ScheduledExecutorService's schedule methods.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService#schedule(java.lang.Runnable, long, edu.emory.mathcs.backport.java.util.concurrent.TimeUnit)
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, edu.emory.mathcs.backport.java.util.concurrent.TimeUnit)
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, edu.emory.mathcs.backport.java.util.concurrent.TimeUnit)
	 */
	public void setScheduledExecutorTasks(ScheduledExecutorTask[] scheduledExecutorTasks) {
		this.scheduledExecutorTasks = scheduledExecutorTasks;
	}

	/**
	 * Specify whether to continue the execution of a scheduled task
	 * after it threw an exception.
	 * <p>Default is "false", matching the native behavior of a
	 * {@link edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService}.
	 * Switch this flag to "true" for exception-proof execution of each task,
	 * continuing scheduled execution as in the case of successful execution.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate
	 */
	public void setContinueScheduledExecutionAfterException(boolean continueScheduledExecutionAfterException) {
		this.continueScheduledExecutionAfterException = continueScheduledExecutionAfterException;
	}

	/**
	 * Set whether to wait for scheduled tasks to complete on shutdown.
	 * <p>Default is "false". Switch this to "true" if you prefer
	 * fully completed tasks at the expense of a longer shutdown phase.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService#shutdown()
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService#shutdownNow()
	 */
	public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	public void setBeanName(String name) {
		this.beanName = name;
	}


	public void afterPropertiesSet() {
		if (logger.isInfoEnabled()) {
			logger.info("Initializing ScheduledExecutorService" +
					(this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		ScheduledExecutorService executor =
				createExecutor(this.poolSize, this.threadFactory, this.rejectedExecutionHandler);

		// Register specified ScheduledExecutorTasks, if necessary.
		if (!ObjectUtils.isEmpty(this.scheduledExecutorTasks)) {
			registerTasks(this.scheduledExecutorTasks, executor);
		}

		// Wrap executor with an unconfigurable decorator.
		this.executor = (this.exposeUnconfigurableExecutor ?
				Executors.unconfigurableScheduledExecutorService(executor) : executor);
	}

	/**
	 * Create a new {@link ScheduledExecutorService} instance.
	 * Called by <code>afterPropertiesSet</code>.
	 * <p>The default implementation creates a {@link ScheduledThreadPoolExecutor}.
	 * Can be overridden in subclasses to provide custom
	 * {@link ScheduledExecutorService} instances.
	 * @param poolSize the specified pool size
	 * @param threadFactory the ThreadFactory to use
	 * @param rejectedExecutionHandler the RejectedExecutionHandler to use
	 * @return a new ScheduledExecutorService instance
	 * @see #afterPropertiesSet()
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor
	 */
	protected ScheduledExecutorService createExecutor(
			int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		return new ScheduledThreadPoolExecutor(poolSize, threadFactory, rejectedExecutionHandler);
	}

	/**
	 * Register the specified {@link ScheduledExecutorTask ScheduledExecutorTasks}
	 * on the given {@link ScheduledExecutorService}.
	 * @param tasks the specified ScheduledExecutorTasks (never empty)
	 * @param executor the ScheduledExecutorService to register the tasks on.
	 */
	protected void registerTasks(ScheduledExecutorTask[] tasks, ScheduledExecutorService executor) {
		for (int i = 0; i < tasks.length; i++) {
			ScheduledExecutorTask task = tasks[i];
			Runnable runnable = getRunnableToSchedule(task);
			if (task.isOneTimeTask()) {
				executor.schedule(runnable, task.getDelay(), task.getTimeUnit());
			}
			else {
				if (task.isFixedRate()) {
					executor.scheduleAtFixedRate(runnable, task.getDelay(), task.getPeriod(), task.getTimeUnit());
				}
				else {
					executor.scheduleWithFixedDelay(runnable, task.getDelay(), task.getPeriod(), task.getTimeUnit());
				}
			}
		}
	}

	/**
	 * Determine the actual Runnable to schedule for the given task.
	 * <p>Wraps the task's Runnable in a
	 * {@link org.springframework.scheduling.support.DelegatingExceptionProofRunnable}
	 * if necessary, according to the
	 * {@link #setContinueScheduledExecutionAfterException "continueScheduledExecutionAfterException"}
	 * flag.
	 * @param task the ScheduledExecutorTask to schedule
	 * @return the actual Runnable to schedule (may be a decorator)
	 */
	protected Runnable getRunnableToSchedule(ScheduledExecutorTask task) {
		boolean propagateException = !this.continueScheduledExecutionAfterException;
		return new DelegatingExceptionProofRunnable(task.getRunnable(), propagateException);
	}


	public Object getObject() {
		return this.executor;
	}

	public Class getObjectType() {
		return (this.executor != null ? this.executor.getClass() : ScheduledExecutorService.class);
	}

	public boolean isSingleton() {
		return true;
	}


	/**
	 * Cancel the ScheduledExecutorService on bean factory shutdown,
	 * stopping all scheduled tasks.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService#shutdown()
	 */
	public void destroy() {
		if (logger.isInfoEnabled()) {
			logger.info("Shutting down ScheduledExecutorService" +
					(this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (this.waitForTasksToCompleteOnShutdown) {
			this.executor.shutdown();
		}
		else {
			this.executor.shutdownNow();
		}
	}

}
