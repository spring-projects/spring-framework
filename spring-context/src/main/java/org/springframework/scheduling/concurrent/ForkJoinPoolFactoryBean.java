/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * A Spring {@link FactoryBean} that builds and exposes a preconfigured {@link ForkJoinPool}.
 * May be used on Java 7 and 8 as well as on Java 6 with {@code jsr166.jar} on the classpath
 * (ideally on the VM bootstrap classpath).
 *
 * <p>For details on the ForkJoinPool API and its use with RecursiveActions, see the
 * <a href="http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ForkJoinPool.html">JDK 7 javadoc</a>.
 *
 * <p>{@code jsr166.jar}, containing {@code java.util.concurrent} updates for Java 6, can be obtained
 * from the <a href="http://gee.cs.oswego.edu/dl/concurrency-interest/">concurrency interest website</a>.
 *
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ForkJoinPoolFactoryBean implements FactoryBean<ForkJoinPool>, InitializingBean, DisposableBean {

	private boolean commonPool = false;

	private int parallelism = Runtime.getRuntime().availableProcessors();

	private ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = ForkJoinPool.defaultForkJoinWorkerThreadFactory;

	private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

	private boolean asyncMode = false;

	private int awaitTerminationSeconds = 0;

	private ForkJoinPool forkJoinPool;


	/**
	 * Set whether to expose JDK 8's 'common' {@link ForkJoinPool}.
	 * <p>Default is "false", creating a local {@link ForkJoinPool} instance based on the
	 * {@link #setParallelism "parallelism"}, {@link #setThreadFactory "threadFactory"},
	 * {@link #setUncaughtExceptionHandler "uncaughtExceptionHandler"} and
	 * {@link #setAsyncMode "asyncMode"} properties on this FactoryBean.
	 * <p><b>NOTE:</b> Setting this flag to "true" effectively ignores all other
	 * properties on this FactoryBean, reusing the shared common JDK {@link ForkJoinPool}
	 * instead. This is a fine choice on JDK 8 but does remove the application's ability
	 * to customize ForkJoinPool behavior, in particular the use of custom threads.
	 * @since 3.2
	 * @see java.util.concurrent.ForkJoinPool#commonPool()
	 */
	public void setCommonPool(boolean commonPool) {
		this.commonPool = commonPool;
	}

	/**
	 * Specify the parallelism level. Default is {@link Runtime#availableProcessors()}.
	 */
	public void setParallelism(int parallelism) {
		this.parallelism = parallelism;
	}

	/**
	 * Set the factory for creating new ForkJoinWorkerThreads.
	 * Default is {@link ForkJoinPool#defaultForkJoinWorkerThreadFactory}.
	 */
	public void setThreadFactory(ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}

	/**
	 * Set the handler for internal worker threads that terminate due to unrecoverable errors
	 * encountered while executing tasks. Default is none.
	 */
	public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
		this.uncaughtExceptionHandler = uncaughtExceptionHandler;
	}

	/**
	 * Specify whether to establish a local first-in-first-out scheduling mode for forked tasks
	 * that are never joined. This mode (asyncMode = {@code true}) may be more appropriate
	 * than the default locally stack-based mode in applications in which worker threads only
	 * process event-style asynchronous tasks. Default is {@code false}.
	 */
	public void setAsyncMode(boolean asyncMode) {
		this.asyncMode = asyncMode;
	}

	/**
	 * Set the maximum number of seconds that this ForkJoinPool is supposed to block
	 * on shutdown in order to wait for remaining tasks to complete their execution
	 * before the rest of the container continues to shut down. This is particularly
	 * useful if your remaining tasks are likely to need access to other resources
	 * that are also managed by the container.
	 * <p>By default, this ForkJoinPool won't wait for the termination of tasks at all.
	 * It will continue to fully execute all ongoing tasks as well as all remaining
	 * tasks in the queue, in parallel to the rest of the container shutting down.
	 * In contrast, if you specify an await-termination period using this property,
	 * this executor will wait for the given time (max) for the termination of tasks.
	 * <p>Note that this feature works for the {@link #setCommonPool "commonPool"}
	 * mode as well. The underlying ForkJoinPool won't actually terminate in that
	 * case but will wait for all tasks to terminate.
	 * @see java.util.concurrent.ForkJoinPool#shutdown()
	 * @see java.util.concurrent.ForkJoinPool#awaitTermination
	 */
	public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
		this.awaitTerminationSeconds = awaitTerminationSeconds;
	}

	@Override
	public void afterPropertiesSet() {
		this.forkJoinPool = (this.commonPool ? ForkJoinPool.commonPool() :
				new ForkJoinPool(this.parallelism, this.threadFactory, this.uncaughtExceptionHandler, this.asyncMode));
	}


	@Override
	public ForkJoinPool getObject() {
		return this.forkJoinPool;
	}

	@Override
	public Class<?> getObjectType() {
		return ForkJoinPool.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		// Ignored for the common pool.
		this.forkJoinPool.shutdown();

		// Wait for all tasks to terminate - works for the common pool as well.
		if (this.awaitTerminationSeconds > 0) {
			try {
				this.forkJoinPool.awaitTermination(this.awaitTerminationSeconds, TimeUnit.SECONDS);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
