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

package org.springframework.scheduling.concurrent;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * JavaBean that allows for configuring a {@link java.util.concurrent.ThreadPoolExecutor}
 * in bean style (through its "corePoolSize", "maxPoolSize", "keepAliveSeconds", "queueCapacity"
 * properties) and exposing it as a Spring {@link org.springframework.core.task.TaskExecutor}.
 * This class is also well suited for management and monitoring (for example, through JMX),
 * providing several useful attributes: "corePoolSize", "maxPoolSize", "keepAliveSeconds"
 * (all supporting updates at runtime); "poolSize", "activeCount" (for introspection only).
 *
 * <p>The default configuration is a core pool size of 1, with unlimited max pool size
 * and unlimited queue capacity. This is roughly equivalent to
 * {@link java.util.concurrent.Executors#newSingleThreadExecutor()}, sharing a single
 * thread for all tasks. Setting {@link #setQueueCapacity "queueCapacity"} to 0 mimics
 * {@link java.util.concurrent.Executors#newCachedThreadPool()}, with immediate scaling
 * of threads in the pool to a potentially very high number. Consider also setting a
 * {@link #setMaxPoolSize "maxPoolSize"} at that point, as well as possibly a higher
 * {@link #setCorePoolSize "corePoolSize"} (see also the
 * {@link #setAllowCoreThreadTimeOut "allowCoreThreadTimeOut"} mode of scaling).
 *
 * <p><b>NOTE:</b> This class implements Spring's
 * {@link org.springframework.core.task.TaskExecutor} interface as well as the
 * {@link java.util.concurrent.Executor} interface, with the former being the primary
 * interface, the other just serving as secondary convenience. For this reason, the
 * exception handling follows the TaskExecutor contract rather than the Executor contract,
 * in particular regarding the {@link org.springframework.core.task.TaskRejectedException}.
 *
 * <p>For an alternative, you may set up a ThreadPoolExecutor instance directly using
 * constructor injection, or use a factory method definition that points to the
 * {@link java.util.concurrent.Executors} class. To expose such a raw Executor as a
 * Spring {@link org.springframework.core.task.TaskExecutor}, simply wrap it with a
 * {@link org.springframework.scheduling.concurrent.ConcurrentTaskExecutor} adapter.
 *
 * @author Juergen Hoeller
 * @author Rémy Guihard
 * @author Sam Brannen
 * @since 2.0
 * @see org.springframework.core.task.TaskExecutor
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see ThreadPoolExecutorFactoryBean
 * @see ConcurrentTaskExecutor
 */
@SuppressWarnings({"serial", "deprecation"})
public class ThreadPoolTaskExecutor extends ExecutorConfigurationSupport
		implements AsyncTaskExecutor, SchedulingTaskExecutor {

	private final Object poolSizeMonitor = new Object();

	private int corePoolSize = 1;

	private int maxPoolSize = Integer.MAX_VALUE;

	private int keepAliveSeconds = 60;

	private int queueCapacity = Integer.MAX_VALUE;

	private boolean allowCoreThreadTimeOut = false;

	private boolean prestartAllCoreThreads = false;

	private boolean strictEarlyShutdown = false;

	private @Nullable TaskDecorator taskDecorator;

	private @Nullable ThreadPoolExecutor threadPoolExecutor;

	// Runnable decorator to user-level FutureTask, if different
	private final Map<Runnable, Object> decoratedTaskMap =
			new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);


	/**
	 * Set the ThreadPoolExecutor's core pool size.
	 * Default is 1.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	public void setCorePoolSize(int corePoolSize) {
		synchronized (this.poolSizeMonitor) {
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setCorePoolSize(corePoolSize);
			}
			this.corePoolSize = corePoolSize;
		}
	}

	/**
	 * Return the ThreadPoolExecutor's core pool size.
	 */
	public int getCorePoolSize() {
		synchronized (this.poolSizeMonitor) {
			return this.corePoolSize;
		}
	}

	/**
	 * Set the ThreadPoolExecutor's maximum pool size.
	 * Default is {@code Integer.MAX_VALUE}.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	public void setMaxPoolSize(int maxPoolSize) {
		synchronized (this.poolSizeMonitor) {
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
			}
			this.maxPoolSize = maxPoolSize;
		}
	}

	/**
	 * Return the ThreadPoolExecutor's maximum pool size.
	 */
	public int getMaxPoolSize() {
		synchronized (this.poolSizeMonitor) {
			return this.maxPoolSize;
		}
	}

	/**
	 * Set the ThreadPoolExecutor's keep-alive seconds.
	 * <p>Default is 60.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		synchronized (this.poolSizeMonitor) {
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
			}
			this.keepAliveSeconds = keepAliveSeconds;
		}
	}

	/**
	 * Return the ThreadPoolExecutor's keep-alive seconds.
	 */
	public int getKeepAliveSeconds() {
		synchronized (this.poolSizeMonitor) {
			return this.keepAliveSeconds;
		}
	}

	/**
	 * Set the capacity for the ThreadPoolExecutor's BlockingQueue.
	 * <p>Default is {@code Integer.MAX_VALUE}.
	 * <p>Any positive value will lead to a LinkedBlockingQueue instance;
	 * any other value will lead to a SynchronousQueue instance.
	 * @see java.util.concurrent.LinkedBlockingQueue
	 * @see java.util.concurrent.SynchronousQueue
	 */
	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	/**
	 * Return the capacity for the ThreadPoolExecutor's BlockingQueue.
	 * @since 5.3.21
	 * @see #setQueueCapacity(int)
	 */
	public int getQueueCapacity() {
		return this.queueCapacity;
	}

	/**
	 * Specify whether to allow core threads to time out. This enables dynamic
	 * growing and shrinking even in combination with a non-zero queue (since
	 * the max pool size will only grow once the queue is full).
	 * <p>Default is "false".
	 * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
	}

	/**
	 * Specify whether to start all core threads, causing them to idly wait for work.
	 * <p>Default is "false", starting threads and adding them to the pool on demand.
	 * @since 5.3.14
	 * @see java.util.concurrent.ThreadPoolExecutor#prestartAllCoreThreads
	 */
	public void setPrestartAllCoreThreads(boolean prestartAllCoreThreads) {
		this.prestartAllCoreThreads = prestartAllCoreThreads;
	}

	/**
	 * Specify whether to initiate an early shutdown signal on context close,
	 * disposing all idle threads and rejecting further task submissions.
	 * <p>By default, existing tasks will be allowed to complete within the
	 * coordinated lifecycle stop phase in any case. This setting just controls
	 * whether an explicit {@link ThreadPoolExecutor#shutdown()} call will be
	 * triggered on context close, rejecting task submissions after that point.
	 * <p>As of 6.1.4, the default is "false", leniently allowing for late tasks
	 * to arrive after context close, still participating in the lifecycle stop
	 * phase. Note that this differs from {@link #setAcceptTasksAfterContextClose}
	 * which completely bypasses the coordinated lifecycle stop phase, with no
	 * explicit waiting for the completion of existing tasks at all.
	 * <p>Switch this to "true" for a strict early shutdown signal analogous to
	 * the 6.1-established default behavior of {@link ThreadPoolTaskScheduler}.
	 * Note that the related flags {@link #setAcceptTasksAfterContextClose} and
	 * {@link #setWaitForTasksToCompleteOnShutdown} will override this setting,
	 * leading to a late shutdown without a coordinated lifecycle stop phase.
	 * @since 6.1.4
	 * @see #initiateShutdown()
	 */
	public void setStrictEarlyShutdown(boolean defaultEarlyShutdown) {
		this.strictEarlyShutdown = defaultEarlyShutdown;
	}

	/**
	 * Specify a custom {@link TaskDecorator} to be applied to any {@link Runnable}
	 * about to be executed.
	 * <p>Note that such a decorator is not necessarily being applied to the
	 * user-supplied {@code Runnable}/{@code Callable} but rather to the actual
	 * execution callback (which may be a wrapper around the user-supplied task).
	 * <p>The primary use case is to set some execution context around the task's
	 * invocation, or to provide some monitoring/statistics for task execution.
	 * <p><b>NOTE:</b> Exception handling in {@code TaskDecorator} implementations
	 * is limited to plain {@code Runnable} execution via {@code execute} calls.
	 * In case of {@code #submit} calls, the exposed {@code Runnable} will be a
	 * {@code FutureTask} which does not propagate any exceptions; you might
	 * have to cast it and call {@code Future#get} to evaluate exceptions.
	 * See the {@code ThreadPoolExecutor#afterExecute} javadoc for an example
	 * of how to access exceptions in such a {@code Future} case.
	 * @since 4.3
	 */
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}


	/**
	 * Note: This method exposes an {@link ExecutorService} to its base class
	 * but stores the actual {@link ThreadPoolExecutor} handle internally.
	 * Do not override this method for replacing the executor, rather just for
	 * decorating its {@code ExecutorService} handle or storing custom state.
	 */
	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);

		ThreadPoolExecutor executor = new ThreadPoolExecutor(
					this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS,
					queue, threadFactory, rejectedExecutionHandler) {
			@Override
			public void execute(Runnable command) {
				Runnable decorated = command;
				if (taskDecorator != null) {
					decorated = taskDecorator.decorate(command);
					if (decorated != command) {
						decoratedTaskMap.put(decorated, command);
					}
				}
				super.execute(decorated);
			}
			@Override
			protected void beforeExecute(Thread thread, Runnable task) {
				ThreadPoolTaskExecutor.this.beforeExecute(thread, task);
			}
			@Override
			protected void afterExecute(Runnable task, Throwable ex) {
				ThreadPoolTaskExecutor.this.afterExecute(task, ex);
			}
		};

		if (this.allowCoreThreadTimeOut) {
			executor.allowCoreThreadTimeOut(true);
		}
		if (this.prestartAllCoreThreads) {
			executor.prestartAllCoreThreads();
		}

		this.threadPoolExecutor = executor;
		return executor;
	}

	/**
	 * Create the BlockingQueue to use for the ThreadPoolExecutor.
	 * <p>A LinkedBlockingQueue instance will be created for a positive
	 * capacity value; a SynchronousQueue otherwise.
	 * @param queueCapacity the specified queue capacity
	 * @return the BlockingQueue instance
	 * @see java.util.concurrent.LinkedBlockingQueue
	 * @see java.util.concurrent.SynchronousQueue
	 */
	protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
		if (queueCapacity > 0) {
			return new LinkedBlockingQueue<>(queueCapacity);
		}
		else {
			return new SynchronousQueue<>();
		}
	}

	/**
	 * Return the underlying ThreadPoolExecutor for native access.
	 * @return the underlying ThreadPoolExecutor (never {@code null})
	 * @throws IllegalStateException if the ThreadPoolTaskExecutor hasn't been initialized yet
	 */
	public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
		Assert.state(this.threadPoolExecutor != null, "ThreadPoolTaskExecutor not initialized");
		return this.threadPoolExecutor;
	}

	/**
	 * Return the current pool size.
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize() {
		if (this.threadPoolExecutor == null) {
			// Not initialized yet: assume core pool size.
			return this.corePoolSize;
		}
		return this.threadPoolExecutor.getPoolSize();
	}

	/**
	 * Return the current queue size.
	 * @since 5.3.21
	 * @see java.util.concurrent.ThreadPoolExecutor#getQueue()
	 */
	public int getQueueSize() {
		if (this.threadPoolExecutor == null) {
			// Not initialized yet: assume no queued tasks.
			return 0;
		}
		return this.threadPoolExecutor.getQueue().size();
	}

	/**
	 * Return the number of currently active threads.
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount() {
		if (this.threadPoolExecutor == null) {
			// Not initialized yet: assume no active threads.
			return 0;
		}
		return this.threadPoolExecutor.getActiveCount();
	}


	@Override
	public void execute(Runnable task) {
		Executor executor = getThreadPoolExecutor();
		try {
			executor.execute(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public Future<?> submit(Runnable task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			return executor.submit(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			return executor.submit(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	protected void cancelRemainingTask(Runnable task) {
		super.cancelRemainingTask(task);
		// Cancel associated user-level Future handle as well
		Object original = this.decoratedTaskMap.get(task);
		if (original instanceof Future<?> future) {
			future.cancel(true);
		}
	}

	@Override
	protected void initiateEarlyShutdown() {
		if (this.strictEarlyShutdown) {
			super.initiateEarlyShutdown();
		}
	}

}
