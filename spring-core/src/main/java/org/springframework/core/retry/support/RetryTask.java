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

package org.springframework.core.retry.support;

import java.util.concurrent.Callable;

import org.jspecify.annotations.Nullable;

import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryOperations;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.core.task.TaskCallback;
import org.springframework.util.Assert;

/**
 * A {@link TaskCallback} that executes a given retryable task,
 * re-executing it after failure according to a retry policy.
 * Inherits {@link Callable} through {@link TaskCallback}.
 *
 * <p>For regular construction, this is designed to match the
 * {@link org.springframework.core.task.SyncTaskExecutor#execute(TaskCallback)}
 * signature for propagating specific exception types but is
 * also usable with the {@code Callback}-based methods on
 * {@link org.springframework.core.task.AsyncTaskExecutor}.
 *
 * <p>Alternatively, this class can also be used to wrap a
 * given {@link Callable} or {@link Runnable} into corresponding
 * retrying variants: see the common static {@code wrap} methods.
 * This is particularly useful for existing {@code Callable} and
 * {@code Runnable} classes, as well as for scheduling methods
 * which typically accept a {@code Runnable} in their signature.
 *
 * @author Juergen Hoeller
 * @since 7.0.4
 * @param <V> the returned value type, if any
 * @param <E> the exception propagated, if any
 * @see RetryTemplate
 * @see org.springframework.core.task.SyncTaskExecutor#execute(TaskCallback)
 * @see org.springframework.core.task.AsyncTaskExecutor#submit(Callable)
 * @see org.springframework.core.task.AsyncTaskExecutor#submitCompletable(Callable)
 */
public class RetryTask<V extends @Nullable Object, E extends Exception> implements TaskCallback<V, E> {

	private final TaskCallback<V, E> task;

	private final RetryOperations retryTemplate;


	/**
	 * Create a new {@code RetryTask} for the given retryable task.
	 * @param task a task that allows for re-execution after failure
	 * @see RetryPolicy#withDefaults()
	 * @see #RetryTask(TaskCallback, RetryPolicy)
	 * @see #RetryTask(TaskCallback, RetryOperations)
	 */
	public RetryTask(TaskCallback<V, E> task) {
		this(task, new RetryTemplate());
	}

	/**
	 * Create a new {@code RetryTask} for the given retryable task.
	 * @param task a task that allows for re-execution after failure
	 * @param retryPolicy the retry policy to apply
	 * @see #RetryTask(TaskCallback, RetryOperations)
	 */
	public RetryTask(TaskCallback<V, E> task, RetryPolicy retryPolicy) {
		this(task, new RetryTemplate(retryPolicy));
	}

	/**
	 * Create a new {@code RetryTask} for the given retryable task.
	 * @param task a task that allows for re-execution after failure
	 * @param retryTemplate the retry delegate to use (typically a {@link RetryTemplate}
	 * but declaring the {@link RetryOperations} interface for flexibility)
	 */
	public RetryTask(TaskCallback<V, E> task, RetryOperations retryTemplate) {
		Assert.notNull(task, "TaskCallback must not be null");
		Assert.notNull(retryTemplate, "RetryTemplate must not be null");
		this.task = task;
		this.retryTemplate = retryTemplate;
	}


	@SuppressWarnings("unchecked")
	@Override
	public V call() throws E {
		try {
			return this.retryTemplate.execute(new Retryable<>() {
				@Override
				public V execute() throws E {
					return task.call();
				}
				@Override
				public String getName() {
					return RetryTask.this.getName();
				}
			});
		}
		catch (RetryException retryException) {
			throw (E) retryException.getCause();
		}
	}

	/**
	 * Return the name of the retryable task:
	 * by default, the class name of the target task.
	 * @see Retryable#getName()
	 */
	public String getName() {
		return this.task.getClass().getName();
	}

	@Override
	public String toString() {
		return "RetryTask for " + getName() + " using " +
				(this.retryTemplate instanceof RetryTemplate rt ? rt.getRetryPolicy() : this.retryTemplate);
	}


	/**
	 * Wrap the given target {@code Callable} into a retrying {@code Callable}.
	 * @param task a task that allows for re-execution after failure
	 * @see RetryPolicy#withDefaults()
	 * @see #wrap(Callable, RetryPolicy)
	 * @see #wrap(Callable, RetryOperations)
	 */
	public static <V> Callable<V> wrap(Callable<V> task) {
		return wrap(task, new RetryTemplate());
	}

	/**
	 * Wrap the given target {@code Callable} into a retrying {@code Callable}.
	 * @param task a task that allows for re-execution after failure
	 * @param retryPolicy the retry policy to apply
	 * @see #wrap(Callable, RetryOperations)
	 */
	public static <V> Callable<V> wrap(Callable<V> task, RetryPolicy retryPolicy) {
		return wrap(task, new RetryTemplate(retryPolicy));
	}

	/**
	 * Wrap the given target {@code Callable} into a retrying {@code Callable}.
	 * @param task a task that allows for re-execution after failure
	 * @param retryTemplate the retry delegate to use (typically a {@link RetryTemplate}
	 * but declaring the {@link RetryOperations} interface for flexibility)
	 */
	public static <V> Callable<V> wrap(Callable<V> task, RetryOperations retryTemplate) {
		return new RetryTask<>(TaskCallback.from(task), retryTemplate) {
			@Override
			public String getName() {
				return task.getClass().getName();
			}
		};
	}

	/**
	 * Wrap the given target {@code Runnable} into a retrying {@code Runnable}.
	 * @param task a task that allows for re-execution after failure
	 * @see RetryPolicy#withDefaults()
	 * @see #wrap(Runnable, RetryPolicy)
	 * @see #wrap(Runnable, RetryOperations)
	 */
	public static Runnable wrap(Runnable task) {
		return wrap(task, new RetryTemplate());
	}

	/**
	 * Wrap the given target {@code Runnable} into a retrying {@code Runnable}.
	 * @param task a task that allows for re-execution after failure
	 * @param retryPolicy the retry policy to apply
	 * @see #wrap(Runnable, RetryOperations)
	 */
	public static Runnable wrap(Runnable task, RetryPolicy retryPolicy) {
		return wrap(task, new RetryTemplate(retryPolicy));
	}

	/**
	 * Wrap the given target {@code Runnable} into a retrying {@code Runnable}.
	 * @param task a task that allows for re-execution after failure
	 * @param retryTemplate the retry delegate to use (typically a {@link RetryTemplate}
	 * but declaring the {@link RetryOperations} interface for flexibility)
	 */
	public static Runnable wrap(Runnable task, RetryOperations retryTemplate) {
		RetryTask<Void, RuntimeException> rt = new RetryTask<>(TaskCallback.from(task), retryTemplate) {
			@Override
			public String getName() {
				return task.getClass().getName();
			}
		};
		return new Runnable() {
			@Override
			public void run() {
				rt.call();
			}
			@Override
			public String toString() {
				return rt.toString();
			}
		};
	}

}
