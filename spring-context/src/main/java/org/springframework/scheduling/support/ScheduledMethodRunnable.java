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

package org.springframework.scheduling.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.CoroutinesUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Variant of {@link MethodInvokingRunnable} meant to be used for processing
 * of no-arg scheduled methods. Propagates user exceptions to the caller,
 * assuming that an error strategy for Runnables is in place.
 *
 * @author Juergen Hoeller
 * @see org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
 * @since 3.0.6
 */
public class ScheduledMethodRunnable implements Runnable {

	private final Object target;

	private final Method method;

	private static final Log logger = LogFactory.getLog(ScheduledMethodRunnable.class);

	/**
	 * Create a {@code ScheduledMethodRunnable} for the given target instance,
	 * calling the specified method.
	 *
	 * @param target the target instance to call the method on
	 * @param method the target method to call
	 */
	public ScheduledMethodRunnable(Object target, Method method) {
		this.target = target;
		this.method = method;
	}

	/**
	 * Create a {@code ScheduledMethodRunnable} for the given target instance,
	 * calling the specified method by name.
	 *
	 * @param target     the target instance to call the method on
	 * @param methodName the name of the target method
	 * @throws NoSuchMethodException if the specified method does not exist
	 */
	public ScheduledMethodRunnable(Object target, String methodName) throws NoSuchMethodException {
		this.target = target;
		this.method = target.getClass().getMethod(methodName);
	}


	/**
	 * Return the target instance to call the method on.
	 */
	public Object getTarget() {
		return this.target;
	}

	/**
	 * Return the target method to call.
	 */
	public Method getMethod() {
		return this.method;
	}


	@Override
	public void run() {
		try {
			ReflectionUtils.makeAccessible(this.method);

			if (KotlinDetector.isKotlinPresent() && KotlinDetector.isSuspendingFunction(method)) {
				CoroutinesUtils.invokeSuspendingFunction(method, target).subscribe(new Subscriber<Object>() {
					@Override
					public void onSubscribe(Subscription s) {
						s.request(Long.MAX_VALUE);
					}

					@Override
					public void onNext(Object o) {

					}

					@Override
					public void onError(Throwable t) {
						logger.error("task run failed", t);
					}

					@Override
					public void onComplete() {

					}
				});
			} else {
				this.method.invoke(this.target);
			}
		} catch (InvocationTargetException ex) {
			ReflectionUtils.rethrowRuntimeException(ex.getTargetException());
		} catch (IllegalAccessException ex) {
			throw new UndeclaredThrowableException(ex);
		}
	}

	@Override
	public String toString() {
		return this.method.getDeclaringClass().getName() + "." + this.method.getName();
	}

}
