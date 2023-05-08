/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.enterprise.concurrent.ManagedExecutors;
import jakarta.enterprise.concurrent.ManagedTask;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Adapter that takes a {@code java.util.concurrent.Executor} and exposes
 * a Spring {@link org.springframework.core.task.TaskExecutor} for it.
 * Also detects an extended {@code java.util.concurrent.ExecutorService}, adapting
 * the {@link org.springframework.core.task.AsyncTaskExecutor} interface accordingly.
 *
 * <p>Autodetects a JSR-236 {@link jakarta.enterprise.concurrent.ManagedExecutorService}
 * in order to expose {@link jakarta.enterprise.concurrent.ManagedTask} adapters for it,
 * exposing a long-running hint based on {@link SchedulingAwareRunnable} and an identity
 * name based on the given Runnable/Callable's {@code toString()}. For JSR-236 style
 * lookup in a Jakarta EE environment, consider using {@link DefaultManagedTaskExecutor}.
 *
 * <p>Note that there is a pre-built {@link ThreadPoolTaskExecutor} that allows
 * for defining a {@link java.util.concurrent.ThreadPoolExecutor} in bean style,
 * exposing it as a Spring {@link org.springframework.core.task.TaskExecutor} directly.
 * This is a convenient alternative to a raw ThreadPoolExecutor definition with
 * a separate definition of the present adapter class.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see java.util.concurrent.Executor
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see java.util.concurrent.Executors
 * @see DefaultManagedTaskExecutor
 * @see ThreadPoolTaskExecutor
 */
@SuppressWarnings("deprecation")
public class ConcurrentTaskExecutor implements AsyncListenableTaskExecutor, SchedulingTaskExecutor {

	private static final Executor STUB_EXECUTOR = (task -> {
		throw new IllegalStateException("Executor not configured");
	});

	@Nullable
	private static Class<?> managedExecutorServiceClass;

	static {
		try {
			managedExecutorServiceClass = ClassUtils.forName(
					"jakarta.enterprise.concurrent.ManagedExecutorService",
					ConcurrentTaskScheduler.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// JSR-236 API not available...
			managedExecutorServiceClass = null;
		}
	}


	private Executor concurrentExecutor = STUB_EXECUTOR;

	private TaskExecutorAdapter adaptedExecutor = new TaskExecutorAdapter(STUB_EXECUTOR);

	@Nullable
	private TaskDecorator taskDecorator;


	/**
	 * Create a new ConcurrentTaskExecutor, using a single thread executor as default.
	 * @see java.util.concurrent.Executors#newSingleThreadExecutor()
	 * @deprecated in favor of {@link #ConcurrentTaskExecutor(Executor)} with an
	 * externally provided Executor
	 */
	@Deprecated(since = "6.1")
	public ConcurrentTaskExecutor() {
		this.concurrentExecutor = Executors.newSingleThreadExecutor();
		this.adaptedExecutor = new TaskExecutorAdapter(this.concurrentExecutor);
	}

	/**
	 * Create a new ConcurrentTaskExecutor, using the given {@link java.util.concurrent.Executor}.
	 * <p>Autodetects a JSR-236 {@link jakarta.enterprise.concurrent.ManagedExecutorService}
	 * in order to expose {@link jakarta.enterprise.concurrent.ManagedTask} adapters for it.
	 * @param executor the {@link java.util.concurrent.Executor} to delegate to
	 */
	public ConcurrentTaskExecutor(@Nullable Executor executor) {
		if (executor != null) {
			setConcurrentExecutor(executor);
		}
	}


	/**
	 * Specify the {@link java.util.concurrent.Executor} to delegate to.
	 * <p>Autodetects a JSR-236 {@link jakarta.enterprise.concurrent.ManagedExecutorService}
	 * in order to expose {@link jakarta.enterprise.concurrent.ManagedTask} adapters for it.
	 */
	public final void setConcurrentExecutor(Executor executor) {
		this.concurrentExecutor = executor;
		this.adaptedExecutor = getAdaptedExecutor(this.concurrentExecutor);
	}

	/**
	 * Return the {@link java.util.concurrent.Executor} that this adapter delegates to.
	 */
	public final Executor getConcurrentExecutor() {
		return this.concurrentExecutor;
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
	 * @since 4.3
	 */
	public final void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
		this.adaptedExecutor.setTaskDecorator(taskDecorator);
	}


	@Override
	public void execute(Runnable task) {
		this.adaptedExecutor.execute(task);
	}

	@Deprecated
	@Override
	public void execute(Runnable task, long startTimeout) {
		this.adaptedExecutor.execute(task, startTimeout);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return this.adaptedExecutor.submit(task);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return this.adaptedExecutor.submit(task);
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		return this.adaptedExecutor.submitListenable(task);
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		return this.adaptedExecutor.submitListenable(task);
	}


	private TaskExecutorAdapter getAdaptedExecutor(Executor concurrentExecutor) {
		if (managedExecutorServiceClass != null && managedExecutorServiceClass.isInstance(concurrentExecutor)) {
			return new ManagedTaskExecutorAdapter(concurrentExecutor);
		}
		TaskExecutorAdapter adapter = new TaskExecutorAdapter(concurrentExecutor);
		if (this.taskDecorator != null) {
			adapter.setTaskDecorator(this.taskDecorator);
		}
		return adapter;
	}


	/**
	 * TaskExecutorAdapter subclass that wraps all provided Runnables and Callables
	 * with a JSR-236 ManagedTask, exposing a long-running hint based on
	 * {@link SchedulingAwareRunnable} and an identity name based on the task's
	 * {@code toString()} representation.
	 */
	private static class ManagedTaskExecutorAdapter extends TaskExecutorAdapter {

		public ManagedTaskExecutorAdapter(Executor concurrentExecutor) {
			super(concurrentExecutor);
		}

		@Override
		public void execute(Runnable task) {
			super.execute(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}

		@Override
		public Future<?> submit(Runnable task) {
			return super.submit(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return super.submit(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}

		@Override
		public ListenableFuture<?> submitListenable(Runnable task) {
			return super.submitListenable(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}

		@Override
		public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
			return super.submitListenable(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}
	}


	/**
	 * Delegate that wraps a given Runnable/Callable  with a JSR-236 ManagedTask,
	 * exposing a long-running hint based on {@link SchedulingAwareRunnable}
	 * and a given identity name.
	 */
	protected static class ManagedTaskBuilder {

		public static Runnable buildManagedTask(Runnable task, String identityName) {
			Map<String, String> properties;
			if (task instanceof SchedulingAwareRunnable schedulingAwareRunnable) {
				properties = new HashMap<>(4);
				properties.put(ManagedTask.LONGRUNNING_HINT,
						Boolean.toString(schedulingAwareRunnable.isLongLived()));
			}
			else {
				properties = new HashMap<>(2);
			}
			properties.put(ManagedTask.IDENTITY_NAME, identityName);
			return ManagedExecutors.managedTask(task, properties, null);
		}

		public static <T> Callable<T> buildManagedTask(Callable<T> task, String identityName) {
			Map<String, String> properties = new HashMap<>(2);
			properties.put(ManagedTask.IDENTITY_NAME, identityName);
			return ManagedExecutors.managedTask(task, properties, null);
		}
	}

}
