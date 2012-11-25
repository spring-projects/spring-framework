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

package org.springframework.scheduling.backportconcurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionHandler;
import edu.emory.mathcs.backport.java.util.concurrent.SynchronousQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;

/**
 * JavaBean that allows for configuring a JSR-166 backport
 * {@link edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor} in bean
 * style (through its "corePoolSize", "maxPoolSize", "keepAliveSeconds", "queueCapacity"
 * properties), exposing it as a Spring {@link org.springframework.core.task.TaskExecutor}.
 * This is an alternative to configuring a ThreadPoolExecutor instance directly using
 * constructor injection, with a separate {@link ConcurrentTaskExecutor} adapter wrapping it.
 *
 * <p>For any custom needs, in particular for defining a
 * {@link edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor},
 * it is recommended to use a straight definition of the Executor instance or a
 * factory method definition that points to the JSR-166 backport
 * {@link edu.emory.mathcs.backport.java.util.concurrent.Executors} class.
 * To expose such a raw Executor as a Spring {@link org.springframework.core.task.TaskExecutor},
 * simply wrap it with a {@link ConcurrentTaskExecutor} adapter.
 *
 * <p><b>NOTE:</b> This class implements Spring's
 * {@link org.springframework.core.task.TaskExecutor} interface (and hence implicitly
 * the standard Java 5 {@link java.util.concurrent.Executor} interface) as well as
 * the JSR-166 {@link edu.emory.mathcs.backport.java.util.concurrent.Executor}
 * interface, with the former being the primary interface, the other just
 * serving as secondary convenience. For this reason, the exception handling
 * follows the TaskExecutor contract rather than the backport Executor contract, in
 * particular regarding the {@link org.springframework.core.task.TaskRejectedException}.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see org.springframework.core.task.TaskExecutor
 * @see edu.emory.mathcs.backport.java.util.concurrent.Executor
 * @see edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor
 * @see edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor
 * @see edu.emory.mathcs.backport.java.util.concurrent.Executors
 * @see ConcurrentTaskExecutor
 * @deprecated as of Spring 3.2, in favor of using the native JDK 6 concurrent support
 */
@Deprecated
public class ThreadPoolTaskExecutor extends CustomizableThreadFactory
		implements SchedulingTaskExecutor, Executor, BeanNameAware, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Object poolSizeMonitor = new Object();

	private int corePoolSize = 1;

	private int maxPoolSize = Integer.MAX_VALUE;

	private int keepAliveSeconds = 60;

	private boolean allowCoreThreadTimeOut = false;

	private int queueCapacity = Integer.MAX_VALUE;

	private ThreadFactory threadFactory = this;

	private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

	private boolean waitForTasksToCompleteOnShutdown = false;

	private boolean threadNamePrefixSet = false;

	private String beanName;

	private ThreadPoolExecutor threadPoolExecutor;


	/**
	 * Set the ThreadPoolExecutor's core pool size.
	 * Default is 1.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	public void setCorePoolSize(int corePoolSize) {
		synchronized (this.poolSizeMonitor) {
			this.corePoolSize = corePoolSize;
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setCorePoolSize(corePoolSize);
			}
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
	 * Default is <code>Integer.MAX_VALUE</code>.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	public void setMaxPoolSize(int maxPoolSize) {
		synchronized (this.poolSizeMonitor) {
			this.maxPoolSize = maxPoolSize;
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
			}
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
	 * Default is 60.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		synchronized (this.poolSizeMonitor) {
			this.keepAliveSeconds = keepAliveSeconds;
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
			}
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
	 * Specify whether to allow core threads to time out. This enables dynamic
	 * growing and shrinking even in combination with a non-zero queue (since
	 * the max pool size will only grow once the queue is full).
	 * <p>Default is "false". Note that this feature is only available on
	 * backport-concurrent 3.0 or above (based on the code in Java 6).
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
	}

	/**
	 * Set the capacity for the ThreadPoolExecutor's BlockingQueue.
	 * Default is <code>Integer.MAX_VALUE</code>.
	 * <p>Any positive value will lead to a LinkedBlockingQueue instance;
	 * any other value will lead to a SynchronousQueue instance.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue
	 * @see edu.emory.mathcs.backport.java.util.concurrent.SynchronousQueue
	 */
	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	/**
	 * Set the ThreadFactory to use for the ThreadPoolExecutor's thread pool.
	 * <p>Default is this executor itself (i.e. the factory that this executor
	 * inherits from). See {@link org.springframework.util.CustomizableThreadCreator}'s
	 * javadoc for available bean properties.
	 * @see #setThreadPriority
	 * @see #setDaemon
	 */
	public void setThreadFactory(ThreadFactory threadFactory) {
		this.threadFactory = (threadFactory != null ? threadFactory : this);
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
	 * Set whether to wait for scheduled tasks to complete on shutdown.
	 * <p>Default is "false". Switch this to "true" if you prefer
	 * fully completed tasks at the expense of a longer shutdown phase.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor#shutdown()
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor#shutdownNow()
	 */
	public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	@Override
	public void setThreadNamePrefix(String threadNamePrefix) {
		super.setThreadNamePrefix(threadNamePrefix);
		this.threadNamePrefixSet = true;
	}

	public void setBeanName(String name) {
		this.beanName = name;
	}


	/**
	 * Calls <code>initialize()</code> after the container applied all property values.
	 * @see #initialize()
	 */
	public void afterPropertiesSet() {
		initialize();
	}

	/**
	 * Creates the BlockingQueue and the ThreadPoolExecutor.
	 * @see #createQueue
	 */
	public void initialize() {
		if (logger.isInfoEnabled()) {
			logger.info("Initializing ThreadPoolExecutor" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (!this.threadNamePrefixSet && this.beanName != null) {
			setThreadNamePrefix(this.beanName + "-");
		}
		BlockingQueue queue = createQueue(this.queueCapacity);
		this.threadPoolExecutor = new ThreadPoolExecutor(
				this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS,
				queue, this.threadFactory, this.rejectedExecutionHandler);
		if (this.allowCoreThreadTimeOut) {
			this.threadPoolExecutor.allowCoreThreadTimeOut(true);
		}
	}

	/**
	 * Create the BlockingQueue to use for the ThreadPoolExecutor.
	 * <p>A LinkedBlockingQueue instance will be created for a positive
	 * capacity value; a SynchronousQueue else.
	 * @param queueCapacity the specified queue capacity
	 * @return the BlockingQueue instance
	 * @see edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue
	 * @see edu.emory.mathcs.backport.java.util.concurrent.SynchronousQueue
	 */
	protected BlockingQueue createQueue(int queueCapacity) {
		if (queueCapacity > 0) {
			return new LinkedBlockingQueue(queueCapacity);
		}
		else {
			return new SynchronousQueue();
		}
	}

	/**
	 * Return the underlying ThreadPoolExecutor for native access.
	 * @return the underlying ThreadPoolExecutor (never <code>null</code>)
	 * @throws IllegalStateException if the ThreadPoolTaskExecutor hasn't been initialized yet
	 */
	public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
		Assert.state(this.threadPoolExecutor != null, "ThreadPoolTaskExecutor not initialized");
		return this.threadPoolExecutor;
	}


	/**
	 * Implementation of both the JSR-166 backport Executor interface and the Spring
	 * TaskExecutor interface, delegating to the ThreadPoolExecutor instance.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.Executor#execute(Runnable)
	 * @see org.springframework.core.task.TaskExecutor#execute(Runnable)
	 */
	public void execute(Runnable task) {
		Executor executor = getThreadPoolExecutor();
		try {
			executor.execute(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	public void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<Object>(task, null);
		execute(future);
		return future;
	}

	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<T>(task);
		execute(future);
		return future;
	}

	/**
	 * This task executor prefers short-lived work units.
	 */
	public boolean prefersShortLivedTasks() {
		return true;
	}


	/**
	 * Return the current pool size.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize() {
		return getThreadPoolExecutor().getPoolSize();
	}

	/**
	 * Return the number of currently active threads.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount() {
		return getThreadPoolExecutor().getActiveCount();
	}


	/**
	 * Calls <code>shutdown</code> when the BeanFactory destroys
	 * the task executor instance.
	 * @see #shutdown()
	 */
	public void destroy() {
		shutdown();
	}

	/**
	 * Perform a shutdown on the ThreadPoolExecutor.
	 * @see edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor#shutdown()
	 */
	public void shutdown() {
		if (logger.isInfoEnabled()) {
			logger.info("Shutting down ThreadPoolExecutor" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (this.waitForTasksToCompleteOnShutdown) {
			this.threadPoolExecutor.shutdown();
		}
		else {
			this.threadPoolExecutor.shutdownNow();
		}
	}

}
