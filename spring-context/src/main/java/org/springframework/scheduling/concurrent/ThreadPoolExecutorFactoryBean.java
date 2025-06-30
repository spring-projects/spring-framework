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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;

/**
 * JavaBean that allows for configuring a {@link java.util.concurrent.ThreadPoolExecutor}
 * in bean style (through its "corePoolSize", "maxPoolSize", "keepAliveSeconds",
 * "queueCapacity" properties) and exposing it as a bean reference of its native
 * {@link java.util.concurrent.ExecutorService} type.
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
 * <p>For an alternative, you may set up a {@link ThreadPoolExecutor} instance directly
 * using constructor injection, or use a factory method definition that points to the
 * {@link java.util.concurrent.Executors} class.
 * <b>This is strongly recommended in particular for common {@code @Bean} methods in
 * configuration classes, where this {@code FactoryBean} variant would force you to
 * return the {@code FactoryBean} type instead of the actual {@code Executor} type.</b>
 *
 * <p>If you need a timing-based {@link java.util.concurrent.ScheduledExecutorService}
 * instead, consider {@link ScheduledExecutorFactoryBean}.

 * @author Juergen Hoeller
 * @since 3.0
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.Executors
 * @see java.util.concurrent.ThreadPoolExecutor
 */
@SuppressWarnings("serial")
public class ThreadPoolExecutorFactoryBean extends ExecutorConfigurationSupport
		implements FactoryBean<ExecutorService> {

	private int corePoolSize = 1;

	private int maxPoolSize = Integer.MAX_VALUE;

	private int keepAliveSeconds = 60;

	private int queueCapacity = Integer.MAX_VALUE;

	private boolean allowCoreThreadTimeOut = false;

	private boolean prestartAllCoreThreads = false;

	private boolean strictEarlyShutdown = false;

	private boolean exposeUnconfigurableExecutor = false;

	private @Nullable ExecutorService exposedExecutor;


	/**
	 * Set the ThreadPoolExecutor's core pool size.
	 * Default is 1.
	 */
	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	/**
	 * Set the ThreadPoolExecutor's maximum pool size.
	 * Default is {@code Integer.MAX_VALUE}.
	 */
	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	/**
	 * Set the ThreadPoolExecutor's keep-alive seconds.
	 * Default is 60.
	 */
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
	}

	/**
	 * Set the capacity for the ThreadPoolExecutor's BlockingQueue.
	 * Default is {@code Integer.MAX_VALUE}.
	 * <p>Any positive value will lead to a LinkedBlockingQueue instance;
	 * any other value will lead to a SynchronousQueue instance.
	 * @see java.util.concurrent.LinkedBlockingQueue
	 * @see java.util.concurrent.SynchronousQueue
	 */
	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
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
	 * <p>Default is "false".
	 * @since 5.3.14
	 * @see java.util.concurrent.ThreadPoolExecutor#prestartAllCoreThreads
	 */
	public void setPrestartAllCoreThreads(boolean prestartAllCoreThreads) {
		this.prestartAllCoreThreads = prestartAllCoreThreads;
	}

	/**
	 * Specify whether to initiate an early shutdown signal on context close,
	 * disposing all idle threads and rejecting further task submissions.
	 * <p>Default is "false".
	 * See {@link ThreadPoolTaskExecutor#setStrictEarlyShutdown} for details.
	 * @since 6.1.4
	 * @see #initiateShutdown()
	 */
	public void setStrictEarlyShutdown(boolean defaultEarlyShutdown) {
		this.strictEarlyShutdown = defaultEarlyShutdown;
	}

	/**
	 * Specify whether this FactoryBean should expose an unconfigurable
	 * decorator for the created executor.
	 * <p>Default is "false", exposing the raw executor as bean reference.
	 * Switch this flag to "true" to strictly prevent clients from
	 * modifying the executor's configuration.
	 * @see java.util.concurrent.Executors#unconfigurableExecutorService
	 */
	public void setExposeUnconfigurableExecutor(boolean exposeUnconfigurableExecutor) {
		this.exposeUnconfigurableExecutor = exposeUnconfigurableExecutor;
	}


	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);
		ThreadPoolExecutor executor = createExecutor(this.corePoolSize, this.maxPoolSize,
				this.keepAliveSeconds, queue, threadFactory, rejectedExecutionHandler);
		if (this.allowCoreThreadTimeOut) {
			executor.allowCoreThreadTimeOut(true);
		}
		if (this.prestartAllCoreThreads) {
			executor.prestartAllCoreThreads();
		}

		// Wrap executor with an unconfigurable decorator.
		this.exposedExecutor = (this.exposeUnconfigurableExecutor ?
				Executors.unconfigurableExecutorService(executor) : executor);

		return executor;
	}

	/**
	 * Create a new instance of {@link ThreadPoolExecutor} or a subclass thereof.
	 * <p>The default implementation creates a standard {@link ThreadPoolExecutor}.
	 * Can be overridden to provide custom {@link ThreadPoolExecutor} subclasses.
	 * @param corePoolSize the specified core pool size
	 * @param maxPoolSize the specified maximum pool size
	 * @param keepAliveSeconds the specified keep-alive time in seconds
	 * @param queue the BlockingQueue to use
	 * @param threadFactory the ThreadFactory to use
	 * @param rejectedExecutionHandler the RejectedExecutionHandler to use
	 * @return a new ThreadPoolExecutor instance
	 * @see #afterPropertiesSet()
	 */
	protected ThreadPoolExecutor createExecutor(
			int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue,
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		return new ThreadPoolExecutor(corePoolSize, maxPoolSize,
				keepAliveSeconds, TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler) {
			@Override
			protected void beforeExecute(Thread thread, Runnable task) {
				ThreadPoolExecutorFactoryBean.this.beforeExecute(thread, task);
			}
			@Override
			protected void afterExecute(Runnable task, Throwable ex) {
				ThreadPoolExecutorFactoryBean.this.afterExecute(task, ex);
			}
		};
	}

	/**
	 * Create the BlockingQueue to use for the ThreadPoolExecutor.
	 * <p>A LinkedBlockingQueue instance will be created for a positive
	 * capacity value; a SynchronousQueue else.
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

	@Override
	protected void initiateEarlyShutdown() {
		if (this.strictEarlyShutdown) {
			super.initiateEarlyShutdown();
		}
	}


	@Override
	public @Nullable ExecutorService getObject() {
		return this.exposedExecutor;
	}

	@Override
	public Class<? extends ExecutorService> getObjectType() {
		return (this.exposedExecutor != null ? this.exposedExecutor.getClass() : ExecutorService.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
