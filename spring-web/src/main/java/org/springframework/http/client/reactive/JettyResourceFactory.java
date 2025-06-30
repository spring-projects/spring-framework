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

package org.springframework.http.client.reactive;


import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.ProcessorUtils;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Factory to manage Jetty resources, i.e. {@link Executor}, {@link ByteBufferPool} and
 * {@link Scheduler}, within the lifecycle of a Spring {@code ApplicationContext}.
 *
 * <p>This factory implements {@link InitializingBean} and {@link DisposableBean}
 * and is expected typically to be declared as a Spring-managed bean.
 *
 * @author Sebastien Deleuze
 * @since 5.1
 */
public class JettyResourceFactory implements InitializingBean, DisposableBean {

	private @Nullable Executor executor;

	private @Nullable ByteBufferPool byteBufferPool;

	private @Nullable Scheduler scheduler;

	private String threadPrefix = "jetty-http";


	/**
	 * Configure the {@link Executor} to use.
	 * <p>By default, initialized with a {@link QueuedThreadPool}.
	 * @param executor the executor to use
	 */
	public void setExecutor(@Nullable Executor executor) {
		this.executor = executor;
	}

	/**
	 * Configure the {@link ByteBufferPool} to use.
	 * <p>By default, initialized with a {@link ArrayByteBufferPool}.
	 * @param byteBufferPool the {@link ByteBuffer} pool to use
	 */
	public void setByteBufferPool(@Nullable ByteBufferPool byteBufferPool) {
		this.byteBufferPool = byteBufferPool;
	}

	/**
	 * Configure the {@link Scheduler} to use.
	 * <p>By default, initialized with a {@link ScheduledExecutorScheduler}.
	 * @param scheduler the {@link Scheduler} to use
	 */
	public void setScheduler(@Nullable Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * Configure the thread prefix to initialize {@link QueuedThreadPool} executor with. This
	 * is used only when a {@link Executor} instance isn't
	 * {@link #setExecutor(Executor) provided}.
	 * <p>By default set to "jetty-http".
	 * @param threadPrefix the thread prefix to use
	 */
	public void setThreadPrefix(String threadPrefix) {
		Assert.notNull(threadPrefix, "Thread prefix is required");
		this.threadPrefix = threadPrefix;
	}

	/**
	 * Return the configured {@link Executor}.
	 */
	public @Nullable Executor getExecutor() {
		return this.executor;
	}

	/**
	 * Return the configured {@link ByteBufferPool}.
	 */
	public @Nullable ByteBufferPool getByteBufferPool() {
		return this.byteBufferPool;
	}

	/**
	 * Return the configured {@link Scheduler}.
	 */
	public @Nullable Scheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		String name = this.threadPrefix + "@" + Integer.toHexString(hashCode());
		if (this.executor == null) {
			QueuedThreadPool threadPool = new QueuedThreadPool();
			threadPool.setName(name);
			this.executor = threadPool;
		}
		if (this.byteBufferPool == null) {
			this.byteBufferPool = new ArrayByteBufferPool(0, 2048, 65536, // from HttpClient:202
					this.executor instanceof ThreadPool.SizedThreadPool sizedThreadPool ?
							sizedThreadPool.getMaxThreads() / 2 :
							ProcessorUtils.availableProcessors() * 2);
		}
		if (this.scheduler == null) {
			this.scheduler = new ScheduledExecutorScheduler(name + "-scheduler", false);
		}

		if (this.executor instanceof LifeCycle lifeCycle) {
			lifeCycle.start();
		}
		this.scheduler.start();
	}

	@Override
	public void destroy() throws Exception {
		try {
			if (this.executor instanceof LifeCycle lifeCycle) {
				lifeCycle.stop();
			}
		}
		catch (Throwable ex) {
			// ignore
		}
		try {
			if (this.scheduler != null) {
				this.scheduler.stop();
			}
		}
		catch (Throwable ex) {
			// ignore
		}
	}

}
