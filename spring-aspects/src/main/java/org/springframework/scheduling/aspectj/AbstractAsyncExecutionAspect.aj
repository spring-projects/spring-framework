/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.scheduling.aspectj;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

/**
 * Abstract aspect that routes selected methods asynchronously.
 *
 * <p>This aspect needs to be injected with an implementation of 
 * {@link Executor} to activate it for a specific thread pool.
 * Otherwise it will simply delegate all calls synchronously.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @since 3.0.5
 */
public abstract aspect AbstractAsyncExecutionAspect {

	private AsyncTaskExecutor asyncExecutor;

	public void setExecutor(Executor executor) {
		if (executor instanceof AsyncTaskExecutor) {
			this.asyncExecutor = (AsyncTaskExecutor) executor;
		}
		else {
			this.asyncExecutor = new TaskExecutorAdapter(executor);
		}
	}

	Object around() : asyncMethod() {
                if (this.asyncExecutor == null) {
			return proceed();
                }
		Callable<Object> callable = new Callable<Object>() {
			public Object call() throws Exception {
				Object result = proceed();
				if (result instanceof Future) {
					return ((Future<?>) result).get();
				}
				return null;
			}};
		Future<?> result = this.asyncExecutor.submit(callable);
		if (Future.class.isAssignableFrom(((MethodSignature) thisJoinPointStaticPart.getSignature()).getReturnType())) {
			return result;
		}
		else {
			return null;
		}
	}

	public abstract pointcut asyncMethod();

}
