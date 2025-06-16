/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.core.task;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.springframework.util.concurrent.FutureUtils;

/**
 * Extended interface for asynchronous {@link TaskExecutor} implementations,
 * offering support for {@link java.util.concurrent.Callable}.
 *
 * <p>Note: The {@link java.util.concurrent.Executors} class includes a set of
 * methods that can convert some other common closure-like objects, for example,
 * {@link java.security.PrivilegedAction} to {@link Callable} before executing them.
 *
 * <p>Implementing this interface also indicates that the {@link #execute(Runnable)}
 * method will not execute its Runnable in the caller's thread but rather
 * asynchronously in some other thread.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see SimpleAsyncTaskExecutor
 * @see org.springframework.scheduling.SchedulingTaskExecutor
 * @see java.util.concurrent.Callable
 * @see java.util.concurrent.Executors
 */
public interface AsyncTaskExecutor extends TaskExecutor {

	/**
	 * Constant that indicates immediate execution.
	 * @deprecated along with {@link #execute(Runnable, long)}
	 */
	@Deprecated(since = "5.3.16")
	long TIMEOUT_IMMEDIATE = 0;

	/**
	 * Constant that indicates no time limit.
	 * @deprecated along with {@link #execute(Runnable, long)}
	 */
	@Deprecated(since = "5.3.16")
	long TIMEOUT_INDEFINITE = Long.MAX_VALUE;


	/**
	 * Execute the given {@code task}.
	 * <p>As of 6.1, this method comes with a default implementation that simply
	 * delegates to {@link #execute(Runnable)}, ignoring the timeout completely.
	 * @param task the {@code Runnable} to execute (never {@code null})
	 * @param startTimeout the time duration (milliseconds) within which the task is
	 * supposed to start. This is intended as a hint to the executor, allowing for
	 * preferred handling of immediate tasks. Typical values are {@link #TIMEOUT_IMMEDIATE}
	 * or {@link #TIMEOUT_INDEFINITE} (the default as used by {@link #execute(Runnable)}).
	 * @throws TaskTimeoutException in case of the task being rejected because
	 * of the timeout (i.e. it cannot be started in time)
	 * @throws TaskRejectedException if the given task was not accepted
	 * @see #execute(Runnable)
	 * @deprecated since the common executors do not support start timeouts
	 */
	@Deprecated(since = "5.3.16")
	default void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	/**
	 * Submit a Runnable task for execution, receiving a Future representing that task.
	 * The Future will return a {@code null} result upon completion.
	 * <p>As of 6.1, this method comes with a default implementation that delegates
	 * to {@link #execute(Runnable)}.
	 * @param task the {@code Runnable} to execute (never {@code null})
	 * @return a Future representing pending completion of the task
	 * @throws TaskRejectedException if the given task was not accepted
	 * @since 3.0
	 */
	default Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<>(task, null);
		execute(future);
		return future;
	}

	/**
	 * Submit a Callable task for execution, receiving a Future representing that task.
	 * The Future will return the Callable's result upon completion.
	 * <p>As of 6.1, this method comes with a default implementation that delegates
	 * to {@link #execute(Runnable)}.
	 * @param task the {@code Callable} to execute (never {@code null})
	 * @return a Future representing pending completion of the task
	 * @throws TaskRejectedException if the given task was not accepted
	 * @since 3.0
	 */
	default <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	/**
	 * Submit a {@code Runnable} task for execution, receiving a {@code CompletableFuture}
	 * representing that task. The Future will return a {@code null} result upon completion.
	 * @param task the {@code Runnable} to execute (never {@code null})
	 * @return a {@code CompletableFuture} representing pending completion of the task
	 * @throws TaskRejectedException if the given task was not accepted
	 * @since 6.0
	 */
	default CompletableFuture<Void> submitCompletable(Runnable task) {
		return CompletableFuture.runAsync(task, this);
	}

	/**
	 * Submit a {@code Callable} task for execution, receiving a {@code CompletableFuture}
	 * representing that task. The Future will return the Callable's result upon
	 * completion.
	 * @param task the {@code Callable} to execute (never {@code null})
	 * @return a {@code CompletableFuture} representing pending completion of the task
	 * @throws TaskRejectedException if the given task was not accepted
	 * @since 6.0
	 */
	default <T> CompletableFuture<T> submitCompletable(Callable<T> task) {
		return FutureUtils.callAsync(task, this);
	}

}
