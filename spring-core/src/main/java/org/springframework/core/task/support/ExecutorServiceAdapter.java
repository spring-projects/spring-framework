/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.task.support;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * Adapter that takes a Spring {@link org.springframework.core.task.TaskExecutor}
 * and exposes a full {@code java.util.concurrent.ExecutorService} for it.
 *
 * <p>This is primarily for adapting to client components that communicate via the
 * {@code java.util.concurrent.ExecutorService} API. It can also be used as
 * common ground between a local Spring {@code TaskExecutor} backend and a
 * JNDI-located {@code ManagedExecutorService} in a Java EE 7 environment.
 *
 * <p><b>NOTE:</b> This ExecutorService adapter does <em>not</em> support the
 * lifecycle methods in the {@code java.util.concurrent.ExecutorService} API
 * ("shutdown()" etc), similar to a server-wide {@code ManagedExecutorService}
 * in a Java EE 7 environment. The lifecycle is always up to the backend pool,
 * with this adapter acting as an access-only proxy for that target pool.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.util.concurrent.ExecutorService
 */
public class ExecutorServiceAdapter extends AbstractExecutorService {

	private final TaskExecutor taskExecutor;


	/**
	 * Create a new ExecutorServiceAdapter, using the given target executor.
	 * @param taskExecutor the target executor to delegate to
	 */
	public ExecutorServiceAdapter(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}


	@Override
	public void execute(Runnable task) {
		this.taskExecutor.execute(task);
	}

	@Override
	public void shutdown() {
		throw new IllegalStateException(
				"Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new IllegalStateException(
				"Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		throw new IllegalStateException(
				"Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

}
