/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.core.task;

import java.io.Serializable;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrencyThrottleSupport;
import org.springframework.util.CustomizableThreadCreator;

/**
 * TaskExecutor implementation that fires up a new Thread for each task,
 * executing it asynchronously.
 *
 * <p>Supports limiting concurrent threads through the "concurrencyLimit"
 * bean property. By default, the number of concurrent threads is unlimited.
 *
 * <p><b>NOTE: This implementation does not reuse threads!</b> Consider a
 * thread-pooling TaskExecutor implementation instead, in particular for
 * executing a large number of short-lived tasks.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setConcurrencyLimit
 * @see SyncTaskExecutor
 * @see org.springframework.scheduling.timer.TimerTaskExecutor
 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
 */
public class SimpleAsyncTaskExecutor extends CustomizableThreadCreator implements AsyncTaskExecutor, Serializable {

	/**
	 * Default thread name prefix: "SimpleAsyncTaskExecutor-".
	 * @deprecated as of Spring 2.0.3, since the default thread name prefix
	 * is now taken from the concrete class (could be a subclass)
	 */
	public static final String DEFAULT_THREAD_NAME_PREFIX =
			ClassUtils.getShortName(SimpleAsyncTaskExecutor.class) + "-";

	/**
	 * Permit any number of concurrent invocations: that is, don't throttle concurrency.
	 */
	public static final int UNBOUNDED_CONCURRENCY = ConcurrencyThrottleSupport.UNBOUNDED_CONCURRENCY;

	/**
	 * Switch concurrency 'off': that is, don't allow any concurrent invocations.
	 */
	public static final int NO_CONCURRENCY = ConcurrencyThrottleSupport.NO_CONCURRENCY;


	/**
	 * Internal concurrency throttle used by this executor.
	 */
	private final ConcurrencyThrottleAdapter concurrencyThrottle = new ConcurrencyThrottleAdapter();


	/**
	 * Create a new SimpleAsyncTaskExecutor with default thread name prefix.
	 */
	public SimpleAsyncTaskExecutor() {
		super();
	}

	/**
	 * Create a new SimpleAsyncTaskExecutor with the given thread name prefix.
	 * @param threadNamePrefix the prefix to use for the names of newly created threads
	 */
	public SimpleAsyncTaskExecutor(String threadNamePrefix) {
		super(threadNamePrefix);
	}


	/**
	 * Set the maximum number of parallel accesses allowed.
	 * -1 indicates no concurrency limit at all.
	 * <p>In principle, this limit can be changed at runtime,
	 * although it is generally designed as a config time setting.
	 * NOTE: Do not switch between -1 and any concrete limit at runtime,
	 * as this will lead to inconsistent concurrency counts: A limit
	 * of -1 effectively turns off concurrency counting completely.
	 * @see #UNBOUNDED_CONCURRENCY
	 */
	public void setConcurrencyLimit(int concurrencyLimit) {
		this.concurrencyThrottle.setConcurrencyLimit(concurrencyLimit);
	}

	/**
	 * Return the maximum number of parallel accesses allowed.
	 */
	public int getConcurrencyLimit() {
		return this.concurrencyThrottle.getConcurrencyLimit();
	}

	/**
	 * Return whether this throttle is currently active.
	 * @return <code>true</code> if the concurrency limit for this instance is active
	 * @see #getConcurrencyLimit()
	 * @see #setConcurrencyLimit
	 */
	public boolean isThrottleActive() {
		return this.concurrencyThrottle.isThrottleActive();
	}


	/**
	 * Executes the given task, within a concurrency throttle
	 * if configured (through the superclass's settings).
	 * @see #doExecute(Runnable)
	 */
	public void execute(Runnable task) {
		execute(task, TIMEOUT_INDEFINITE);
	}

	/**
	 * Executes the given task, within a concurrency throttle
	 * if configured (through the superclass's settings).
	 * <p>Executes urgent tasks (with 'immediate' timeout) directly,
	 * bypassing the concurrency throttle (if active). All other
	 * tasks are subject to throttling.
	 * @see #TIMEOUT_IMMEDIATE
	 * @see #doExecute(Runnable)
	 */
	public void execute(Runnable task, long startTimeout) {
		Assert.notNull(task, "Runnable must not be null");
		if (isThrottleActive() && startTimeout > TIMEOUT_IMMEDIATE) {
			this.concurrencyThrottle.beforeAccess();
			doExecute(new ConcurrencyThrottlingRunnable(task));
		}
		else {
			doExecute(task);
		}
	}

	/**
	 * Template method for the actual execution of a task.
	 * <p>The default implementation creates a new Thread and starts it.
	 * @param task the Runnable to execute
	 * @see #createThread
	 * @see java.lang.Thread#start()
	 */
	protected void doExecute(Runnable task) {
		createThread(task).start();
	}


	/**
	 * Subclass of the general ConcurrencyThrottleSupport class,
	 * making <code>beforeAccess()</code> and <code>afterAccess()</code>
	 * visible to the surrounding class.
	 */
	private static class ConcurrencyThrottleAdapter extends ConcurrencyThrottleSupport {

		protected void beforeAccess() {
			super.beforeAccess();
		}

		protected void afterAccess() {
			super.afterAccess();
		}
	}


	/**
	 * This Runnable calls <code>afterAccess()</code> after the
	 * target Runnable has finished its execution.
	 */
	private class ConcurrencyThrottlingRunnable implements Runnable {

		private final Runnable target;

		public ConcurrencyThrottlingRunnable(Runnable target) {
			this.target = target;
		}

		public void run() {
			try {
				this.target.run();
			}
			finally {
				concurrencyThrottle.afterAccess();
			}
		}
	}

}
