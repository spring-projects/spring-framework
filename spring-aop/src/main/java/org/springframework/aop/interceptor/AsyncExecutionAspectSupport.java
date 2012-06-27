/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for asynchronous method execution aspects, such as
 * {@link org.springframework.scheduling.annotation.AnnotationAsyncExecutionInterceptor}
 * or {@link org.springframework.scheduling.aspectj.AnnotationAsyncExecutionAspect}.
 *
 * <p>Provides support for <i>executor qualification</i> on a method-by-method basis.
 * {@code AsyncExecutionAspectSupport} objects must be constructed with a default {@code
 * Executor}, but each individual method may further qualify a specific {@code Executor}
 * bean to be used when executing it, e.g. through an annotation attribute.
 *
 * @author Chris Beams
 * @since 3.1.2
 */
public abstract class AsyncExecutionAspectSupport implements BeanFactoryAware {

	private final Map<Method, AsyncTaskExecutor> executors = new HashMap<Method, AsyncTaskExecutor>();

	private Executor defaultExecutor;

	private BeanFactory beanFactory;


	/**
	 * Create a new {@link AsyncExecutionAspectSupport}, using the provided default
	 * executor unless individual async methods indicate via qualifier that a more
	 * specific executor should be used.
	 * @param defaultExecutor the executor to use when executing asynchronous methods
	 */
	public AsyncExecutionAspectSupport(Executor defaultExecutor) {
		this.setExecutor(defaultExecutor);
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
	 * Set the {@link BeanFactory} to be used when looking up executors by qualifier.
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
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
	 * Determine the specific executor to use when executing the given method.
	 * @returns the executor to use (never {@code null})
	 */
	protected AsyncTaskExecutor determineAsyncExecutor(Method method) {
		if (!this.executors.containsKey(method)) {
			Executor executor = this.defaultExecutor;

			String qualifier = getExecutorQualifier(method);
			if (StringUtils.hasLength(qualifier)) {
				Assert.notNull(this.beanFactory,
						"BeanFactory must be set on " + this.getClass().getSimpleName() +
						" to access qualified executor [" + qualifier + "]");
				executor = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
						this.beanFactory, Executor.class, qualifier);
			}

			if (executor instanceof AsyncTaskExecutor) {
				this.executors.put(method, (AsyncTaskExecutor) executor);
			}
			else if (executor instanceof Executor) {
				this.executors.put(method, new TaskExecutorAdapter(executor));
			}
		}

		return this.executors.get(method);
	}

}
