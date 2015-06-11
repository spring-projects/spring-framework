/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.interceptor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for asynchronous method execution aspects, such as
 * {@code org.springframework.scheduling.annotation.AnnotationAsyncExecutionInterceptor}
 * or {@code org.springframework.scheduling.aspectj.AnnotationAsyncExecutionAspect}.
 *
 * <p>Provides support for <i>executor qualification</i> on a method-by-method basis.
 * {@code AsyncExecutionAspectSupport} objects must be constructed with a default {@code
 * Executor}, but each individual method may further qualify a specific {@code Executor}
 * bean to be used when executing it, e.g. through an annotation attribute.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1.2
 */
public abstract class AsyncExecutionAspectSupport implements BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<Method, AsyncTaskExecutor> executors = new ConcurrentHashMap<Method, AsyncTaskExecutor>(16);

	private Executor defaultExecutor;

	private AsyncUncaughtExceptionHandler exceptionHandler;

	private BeanFactory beanFactory;


	/**
	 * Create a new {@link AsyncExecutionAspectSupport}, using the provided default
	 * executor unless individual async methods indicate via qualifier that a more
	 * specific executor should be used.
	 * @param defaultExecutor the executor to use when executing asynchronous methods
	 * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use
	 */
	public AsyncExecutionAspectSupport(Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
		this.defaultExecutor = defaultExecutor;
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Create a new instance with a default {@link AsyncUncaughtExceptionHandler}.
	 */
	public AsyncExecutionAspectSupport(Executor defaultExecutor) {
		this(defaultExecutor, new SimpleAsyncUncaughtExceptionHandler());
	}


	/**
	 * Supply the executor to be used when executing async methods.
	 * @param defaultExecutor the {@code Executor} (typically a Spring {@code
	 * AsyncTaskExecutor} or {@link java.util.concurrent.ExecutorService}) to delegate to
	 * unless a more specific executor has been requested via a qualifier on the async
	 * method, in which case the executor will be looked up at invocation time against the
	 * enclosing bean factory.
	 * @see #getExecutorQualifier
	 * @see #setBeanFactory(BeanFactory)
	 */
	public void setExecutor(Executor defaultExecutor) {
		this.defaultExecutor = defaultExecutor;
	}

	/**
	 * Supply the {@link AsyncUncaughtExceptionHandler} to use to handle exceptions
	 * thrown by invoking asynchronous methods with a {@code void} return type.
	 */
	public void setExceptionHandler(AsyncUncaughtExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Set the {@link BeanFactory} to be used when looking up executors by qualifier.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	/**
	 * Determine the specific executor to use when executing the given method.
	 * Should preferably return an {@link AsyncListenableTaskExecutor} implementation.
	 * @return the executor to use (or {@code null}, but just if no default executor has been set)
	 */
	protected AsyncTaskExecutor determineAsyncExecutor(Method method) {
		AsyncTaskExecutor executor = this.executors.get(method);
		if (executor == null) {
			Executor executorToUse = this.defaultExecutor;
			String qualifier = getExecutorQualifier(method);
			if (StringUtils.hasLength(qualifier)) {
				Assert.notNull(this.beanFactory, "BeanFactory must be set on " + getClass().getSimpleName() +
						" to access qualified executor '" + qualifier + "'");
				executorToUse = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
						this.beanFactory, Executor.class, qualifier);
			}
			else if (executorToUse == null) {
				return null;
			}
			executor = (executorToUse instanceof AsyncListenableTaskExecutor ?
					(AsyncListenableTaskExecutor) executorToUse : new TaskExecutorAdapter(executorToUse));
			this.executors.put(method, executor);
		}
		return executor;
	}

	/**
	 * Return the qualifier or bean name of the executor to be used when executing the
	 * given async method, typically specified in the form of an annotation attribute.
	 * Returning an empty string or {@code null} indicates that no specific executor has
	 * been specified and that the {@linkplain #setExecutor(Executor) default executor}
	 * should be used.
	 * @param method the method to inspect for executor qualifier metadata
	 * @return the qualifier if specified, otherwise empty string or {@code null}
	 * @see #determineAsyncExecutor(Method)
	 */
	protected abstract String getExecutorQualifier(Method method);

	/**
	 * Handles a fatal error thrown while asynchronously invoking the specified
	 * {@link Method}.
	 * <p>If the return type of the method is a {@link Future} object, the original
	 * exception can be propagated by just throwing it at the higher level. However,
	 * for all other cases, the exception will not be transmitted back to the client.
	 * In that later case, the current {@link AsyncUncaughtExceptionHandler} will be
	 * used to manage such exception.
	 * @param ex the exception to handle
	 * @param method the method that was invoked
	 * @param params the parameters used to invoke the method
	 */
	protected void handleError(Throwable ex, Method method, Object... params) throws Exception {
		if (Future.class.isAssignableFrom(method.getReturnType())) {
			ReflectionUtils.rethrowException(ex);
		}
		else {
			// Could not transmit the exception to the caller with default executor
			try {
				this.exceptionHandler.handleUncaughtException(ex, method, params);
			}
			catch (Throwable ex2) {
				logger.error("Exception handler for async method '" + method.toGenericString() +
						"' threw unexpected exception itself", ex2);
			}
		}
	}

}
