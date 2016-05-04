/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.simp.config;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * A registration class for customizing the properties of {@link ThreadPoolTaskExecutor}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class TaskExecutorRegistration {

	private ThreadPoolTaskExecutor taskExecutor;

	private int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;

	private int maxPoolSize = Integer.MAX_VALUE;

	private int queueCapacity = Integer.MAX_VALUE;

	private int keepAliveSeconds = 60;


	public TaskExecutorRegistration() {
	}

	public TaskExecutorRegistration(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Set the core pool size of the ThreadPoolExecutor.
	 * <p><strong>NOTE:</strong> The core pool size is effectively the max pool size
	 * when an unbounded {@link #queueCapacity(int) queueCapacity} is configured
	 * (the default). This is essentially the "Unbounded queues" strategy as explained
	 * in {@link java.util.concurrent.ThreadPoolExecutor ThreadPoolExecutor}. When
	 * this strategy is used, the {@link #maxPoolSize(int) maxPoolSize} is ignored.
	 * <p>By default this is set to twice the value of
	 * {@link Runtime#availableProcessors()}. In an application where tasks do not
	 * block frequently, the number should be closer to or equal to the number of
	 * available CPUs/cores.
	 */
	public TaskExecutorRegistration corePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
		return this;
	}

	/**
	 * Set the max pool size of the ThreadPoolExecutor.
	 * <p><strong>NOTE:</strong> When an unbounded
	 * {@link #queueCapacity(int) queueCapacity} is configured (the default), the
	 * max pool size is effectively ignored. See the "Unbounded queues" strategy
	 * in {@link java.util.concurrent.ThreadPoolExecutor ThreadPoolExecutor} for
	 * more details.
	 * <p>By default this is set to {@code Integer.MAX_VALUE}.
	 */
	public TaskExecutorRegistration maxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
		return this;
	}

	/**
	 * Set the queue capacity for the ThreadPoolExecutor.
	 * <p><strong>NOTE:</strong> when an unbounded {@code queueCapacity} is configured
	 * (the default), the core pool size is effectively the max pool size. This is
	 * essentially the "Unbounded queues" strategy as explained in
	 * {@link java.util.concurrent.ThreadPoolExecutor ThreadPoolExecutor}. When
	 * this strategy is used, the {@link #maxPoolSize(int) maxPoolSize} is ignored.
	 * <p>By default this is set to {@code Integer.MAX_VALUE}.
	 */
	public TaskExecutorRegistration queueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
		return this;
	}

	/**
	 * Set the time limit for which threads may remain idle before being terminated.
	 * If there are more than the core number of threads currently in the pool,
	 * after waiting this amount of time without processing a task, excess threads
	 * will be terminated. This overrides any value set in the constructor.
	 * <p>By default this is set to 60.
	 */
	public TaskExecutorRegistration keepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
		return this;
	}

	protected ThreadPoolTaskExecutor getTaskExecutor() {
		ThreadPoolTaskExecutor executor = (this.taskExecutor != null ? this.taskExecutor : new ThreadPoolTaskExecutor());
		executor.setCorePoolSize(this.corePoolSize);
		executor.setMaxPoolSize(this.maxPoolSize);
		executor.setKeepAliveSeconds(this.keepAliveSeconds);
		executor.setQueueCapacity(this.queueCapacity);
		executor.setAllowCoreThreadTimeOut(true);
		return executor;
	}

}
