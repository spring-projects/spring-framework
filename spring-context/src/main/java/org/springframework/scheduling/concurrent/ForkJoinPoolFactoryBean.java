/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * A Spring {@link FactoryBean} that builds and exposes a preconfigured {@link ForkJoinPool}.
 * May be used on Java 7 as well as on Java 6 with <code>jsr166.jar</code> on the classpath
 * (ideally on the VM bootstrap classpath).
 *
 * <p>For details on the ForkJoinPool API and its use with RecursiveActions, see the
 * <a href="http://download.java.net/jdk7/docs/api/java/util/concurrent/ForkJoinPool.html">JDK 7 javadoc</a>.
 *
 * <p><code>jsr166.jar</code>, containing <code>java.util.concurrent</code> updates for Java 6, can be obtained
 * from the <a href="http://gee.cs.oswego.edu/dl/concurrency-interest/">concurrency interest website</a>.
 *
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ForkJoinPoolFactoryBean implements FactoryBean<ForkJoinPool>, InitializingBean, DisposableBean {

	private int parallelism = Runtime.getRuntime().availableProcessors();

	private ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = ForkJoinPool.defaultForkJoinWorkerThreadFactory;

	private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

	private boolean asyncMode = false;

	private ForkJoinPool forkJoinPool;


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
	 * that are never joined. This mode (asyncMode = <code>true</code>) may be more appropriate
	 * than the default locally stack-based mode in applications in which worker threads only
	 * process event-style asynchronous tasks. Default is <code>false</code>.
	 */
	public void setAsyncMode(boolean asyncMode) {
		this.asyncMode = asyncMode;
	}

	public void afterPropertiesSet() {
		this.forkJoinPool =
				new ForkJoinPool(this.parallelism, this.threadFactory, this.uncaughtExceptionHandler, this.asyncMode);
	}


	public ForkJoinPool getObject() {
		return this.forkJoinPool;
	}

	public Class<?> getObjectType() {
		return ForkJoinPool.class;
	}

	public boolean isSingleton() {
		return true;
	}


	public void destroy() {
		this.forkJoinPool.shutdown();
	}

}
