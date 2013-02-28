/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.task;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.util.Assert;

/**
 * Miscellaneous utilities for dealing with operations.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public abstract class AsyncUtils {

	/**
	 * Submit a method call via an existing object for asynchronous execution. A proxy
	 * of the specified source object is returned where calls to methods on that object
	 * result in submission of a new executor task. Any non-void methods will return a
	 * {@link #proxyFutureResult(Future) proxy} for the future result.
	 *
	 * <p>This method cab be particularly useful to speed up initialization code,
	 * for example:
	 *
	 * <pre>{@code
	 *   PersistenceProvider provider = AsyncUtils.submitVia(executorService, persistenceProvider);
	 *   // This is usually a slow operation
	 *   managerFactory = provider.createContainerEntityManagerFactory();
	 *   // a reference to managerFacory can be held indefinitely.
	 *   // If no methods are called the thread is not blocked.
	 * }</code>
	 *
	 * <p>This method can only be used with non-final non-private classes and methods.
	 * @param executor the executor used to run the task (may be {@code null} to execute
	 * in the current thread)
	 * @param source the source object
	 * @return a proxy of the source object that can be used to submit tasks
	 * @see #submitVia(AsyncTaskExecutor, Object, Class)
	 * @see #proxyFutureResult(Future)
	 */
	public static <T> T submitVia(AsyncTaskExecutor executor, T source) {
		return submitVia(asExecutorService(executor), source);
	}

	/**
	 * Submit a method call via an existing object for asynchronous execution. See
	 * {@link #submitVia(AsyncTaskExecutor, Object)} for details.
	 * @param executor the executor used to run the task (may be {@code null} to execute
	 * in the current thread)
	 * @param source the source object
	 * @return a proxy of the source object that can be used to submit tasks
	 */
	@SuppressWarnings("unchecked")
	public static <T> T submitVia(ExecutorService executor, T source) {
		return submitVia(executor, source, (Class<T>) source.getClass());
	}

	/**
	 * Submit a method call via an existing object for asynchronous execution. See
	 * {@link #submitVia(AsyncTaskExecutor, Object)} for details.
	 * @param executor the executor used to run the task (may be {@code null} to execute
	 * in the current thread)
	 * @param source the source object
	 * @param resultType The type of proxy to result
	 * @return a proxy of the source object that can be used to submit tasks
	 */
	public static <T> T submitVia(AsyncTaskExecutor executor, T source, final Class<T> resultType) {
		return submitVia(new ExecutorServiceAdapter(executor), source, resultType);
	}

	/**
	 * Submit a method call via an existing object for asynchronous execution. See
	 * {@link #submitVia(AsyncTaskExecutor, Object)} for details.
	 * @param executor the executor used to run the task (may be {@code null} to execute
	 * in the current thread)
	 * @param source the source object
	 * @param resultType The type of proxy to result
	 * @return a proxy of the source object that can be used to submit tasks
	 */
	@SuppressWarnings("unchecked")
	public static <T> T submitVia(final ExecutorService executor, T source, final Class<T> resultType) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(resultType, "ResultType must not be null");

		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(resultType);
		enhancer.setCallback(new MethodInterceptor() {
			public Object intercept(final Object obj, Method method, final Object[] args,
					final MethodProxy proxy) throws Throwable {
				Class returnType = method.getReturnType();
				if(executor == null) {
					return proxy.invokeSuper(obj, args);
				}
				Future<?> future = executor.submit(new Callable<Object>() {
					public Object call() throws Exception {
						try {
							return proxy.invokeSuper(obj, args);
						}
						catch (Throwable ex) {
							if(ex instanceof Exception) {
								throw (Exception) ex;
							}
							throw new RuntimeException(ex);
						}
					}
				});
				return proxyFutureResult(future, returnType);
			}
		});
		return (T) enhancer.create();
	}

	/**
	 * Submit the specified task to the given executor, or if the executor is {@code null}
	 * run the task immediately.
	 * @param executor the executor (may be {@code null}
	 * @param task the task to execute
	 * @return the future result
	 */
	public static <T> Future<T> submit(AsyncTaskExecutor executor, Callable<T> task) {
		return submit(asExecutorService(executor), task);
	}

	/**
	 * Submit the specified task to the given executor, or if the executor is {@code null}
	 * run the task immediately.
	 * @param executor the executor (may be {@code null}
	 * @param task the task to execute
	 * @return the future result
	 */
	public static <T> Future<T> submit(ExecutorService executor, Callable<T> task) {
		Assert.notNull(task, "Task must not be null");
		return (executor == null ? new CompletedFuture<T>(task) : executor.submit(task));
	}

	/**
	 * Submit the specified task to the given executor, or if the executor is {@code null}
	 * run the task immediately.
	 * @param executor the executor (may be {@code null}
	 * @param task the task to execute
	 * @return the future result
	 */
	public static Future<?> submit(AsyncTaskExecutor executor, Runnable task) {
		return submit(asExecutorService(executor), task);
	}

	/**
	 * Submit the specified task to the given executor, or if the executor is {@code null}
	 * run the task immediately.
	 * @param executor the executor (may be {@code null}
	 * @param task the task to execute
	 * @return the future result
	 */
	public static Future<?> submit(ExecutorService executor, Runnable task) {
		Assert.notNull(task, "Task must not be null");
		return (executor == null ? new CompletedFuture<Object>(Executors.callable(task)) : executor.submit(task));
	}

	/**
	 * Submit the specified task to the given executor, or if the executor is {@code null}
	 * run the task immediately.
	 * @param executor the executor (may be {@code null}
	 * @param task the task to execute
	 * @param result the result
	 * @return the future result
	 */
	public static <T> Future<T> submit(AsyncTaskExecutor executor, Runnable task, T result) {
		return submit(asExecutorService(executor), task, result);
	}

	/**
	 * Submit the specified task to the given executor, or if the executor is {@code null}
	 * run the task immediately.
	 * @param executor the executor (may be {@code null}
	 * @param task the task to execute
	 * @param result the result
	 * @return the future result
	 */
	public static <T> Future<T> submit(ExecutorService executor, Runnable task, T result) {
		Assert.notNull(task, "Task must not be null");
		return (executor == null ? new CompletedFuture<T>(Executors.callable(task, result)) : executor.submit(task, result));
	}

	/**
	 * Return a proxy for some Future result. Calls on the proxy will cause the
	 * {@link Future#get()} method to be called.
	 * @param future the future
	 * @return a proxy of the future type {@code <T>}
	 */
	@SuppressWarnings("unchecked")
	public static <T> T proxyFutureResult(Future<T> future) {
		Assert.notNull(future, "Future must not be null");
		Class<?> type = GenericTypeResolver.resolveTypeArgument(future.getClass(),
				Future.class);
		Assert.notNull(type, "Unable to resolve generic type for Future");
		return proxyFutureResult(future, (Class<T>) type);
	}

	/**
	 * Return a proxy for some Future result. Calls on the proxy will cause the
	 * {@link Future#get()} method to be called.
	 * @param future the future
	 * @param the result type
	 * @return a proxy of the future type {@code <T>}
	 */
	@SuppressWarnings("unchecked")
	public static <T> T proxyFutureResult(final Future<T> future, Class<? super T> resultType) {
		Assert.notNull(future, "Future must not be null");
		Assert.notNull(resultType, "ResultType must not be null");

		// No need to proxy an already completed future
		if (future.isDone()) {
			return get(future);
		}

		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(resultType);
		enhancer.setCallback(new MethodInterceptor() {
			public Object intercept(Object obj, Method method, Object[] args,
					MethodProxy proxy) throws Throwable {
				return method.invoke(get(future), args);
			}
		});

		try {
			return (T) enhancer.create();
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unable to proxy future result", ex);
		}
	}

	/**
	 * Calls {@link Future#get()}, wrapping any checked exceptions in
	 * {@link RuntimeException}.
	 * @param future the future
	 * @return the result of {@link Future#get()}
	 */
	public static <T> T get(Future<T> future) {
		Assert.notNull(future, "Future must not be null");
		try {
			return future.get();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(ex);
		}
		catch (ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Adapt a {@link AsyncTaskExecutor} to a {@link ExecutorService}.
	 * @param executor the {@link AsyncTaskExecutor} (may be {@code null})
	 * @return an {@link ExecutorService} or {@code null} of the source executor was {@code null}
	 */
	private static ExecutorService asExecutorService(AsyncTaskExecutor executor) {
		return (executor == null ? null : new ExecutorServiceAdapter(executor));
	}


	/**
	 * Simple {@link Future} implementation that resolves a task immediately.
	 * @param <T> The type
	 */
	private static class CompletedFuture<T> implements Future<T> {

		private T result;

		private ExecutionException exception;


		public CompletedFuture(Callable<T> task) {
			try {
				this.result = task.call();
			}
			catch (Exception ex) {
				this.exception = new ExecutionException(ex);
			}
		}


		public boolean cancel(boolean mayInterruptIfRunning) {
			return true;
		}

		public boolean isCancelled() {
			return false;
		}

		public boolean isDone() {
			return true;
		}

		public T get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			return get();
		}

		public T get() throws InterruptedException, ExecutionException {
			if(this.exception != null) {
				throw this.exception;
			}
			return result;
		}
	}
}
