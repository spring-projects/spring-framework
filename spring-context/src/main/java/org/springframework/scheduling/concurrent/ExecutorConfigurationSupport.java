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

package org.springframework.scheduling.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Base class for classes that are setting up a
 * {@code java.util.concurrent.ExecutorService}
 * (typically a {@link java.util.concurrent.ThreadPoolExecutor}).
 * Defines common configuration settings and common lifecycle handling.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.Executors
 * @see java.util.concurrent.ThreadPoolExecutor
 */
public abstract class ExecutorConfigurationSupport extends CustomizableThreadFactory
		implements BeanNameAware, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ThreadFactory threadFactory = this;

	private boolean threadNamePrefixSet = false;

	private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

	private boolean waitForTasksToCompleteOnShutdown = false;

	private String beanName;

	private ExecutorService executor;


	/**
	 * Set the ThreadFactory to use for the ThreadPoolExecutor's thread pool.
	 * Default is the ThreadPoolExecutor's default thread factory.
	 * @see java.util.concurrent.Executors#defaultThreadFactory()
	 */
	public void setThreadFactory(ThreadFactory threadFactory) {
		this.threadFactory = (threadFactory != null ? threadFactory : this);
	}

	@Override
	public void setThreadNamePrefix(String threadNamePrefix) {
		super.setThreadNamePrefix(threadNamePrefix);
		this.threadNamePrefixSet = true;
	}

	/**
	 * Set the RejectedExecutionHandler to use for the ThreadPoolExecutor.
	 * Default is the ThreadPoolExecutor's default abort policy.
	 * @see java.util.concurrent.ThreadPoolExecutor.AbortPolicy
	 */
	public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
		this.rejectedExecutionHandler =
				(rejectedExecutionHandler != null ? rejectedExecutionHandler : new ThreadPoolExecutor.AbortPolicy());
	}

	/**
	 * Set whether to wait for scheduled tasks to complete on shutdown.
	 * <p>Default is "false". Switch this to "true" if you prefer
	 * fully completed tasks at the expense of a longer shutdown phase.
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 */
	public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	public void setBeanName(String name) {
		this.beanName = name;
	}


	/**
	 * Calls {@code initialize()} after the container applied all property values.
	 * @see #initialize()
	 */
	public void afterPropertiesSet() {
		initialize();
	}

	/**
	 * Set up the ExecutorService.
	 */
	public void initialize() {
		if (logger.isInfoEnabled()) {
			logger.info("Initializing ExecutorService " + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (!this.threadNamePrefixSet && this.beanName != null) {
			setThreadNamePrefix(this.beanName + "-");
		}
		this.executor = initializeExecutor(this.threadFactory, this.rejectedExecutionHandler);
	}

	/**
	 * Create the target {@link java.util.concurrent.ExecutorService} instance.
	 * Called by {@code afterPropertiesSet}.
	 * @param threadFactory the ThreadFactory to use
	 * @param rejectedExecutionHandler the RejectedExecutionHandler to use
	 * @return a new ExecutorService instance
	 * @see #afterPropertiesSet()
	 */
	protected abstract ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler);


	/**
	 * Calls {@code shutdown} when the BeanFactory destroys
	 * the task executor instance.
	 * @see #shutdown()
	 */
	public void destroy() {
		shutdown();
	}

	/**
	 * Perform a shutdown on the ThreadPoolExecutor.
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 */
	public void shutdown() {
		if (logger.isInfoEnabled()) {
			logger.info("Shutting down ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (this.waitForTasksToCompleteOnShutdown) {
			this.executor.shutdown();
		}
		else {
			this.executor.shutdownNow();
		}
	}

}
